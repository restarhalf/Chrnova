package restarhalf.stellar.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.image.toAsyncImageModel
import restarhalf.stellar.schedule.ui.navigation.AppBottomBar
import restarhalf.stellar.schedule.ui.navigation.AppChromeState
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.AppScaffoldBody
import restarhalf.stellar.schedule.ui.navigation.LocalAppChromeState
import restarhalf.stellar.schedule.ui.navigation.LocalMainPagerState
import restarhalf.stellar.schedule.ui.navigation.LocalNavigator
import restarhalf.stellar.schedule.ui.navigation.RootTabs
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.navigation.TransparentStackTransition
import restarhalf.stellar.schedule.ui.navigation.graph.AppNavDeps
import restarhalf.stellar.schedule.ui.navigation.graph.announcementEntries
import restarhalf.stellar.schedule.ui.navigation.graph.courseSelectionEntries
import restarhalf.stellar.schedule.ui.navigation.graph.evaluationEntries
import restarhalf.stellar.schedule.ui.navigation.graph.miscEntries
import restarhalf.stellar.schedule.ui.navigation.graph.papersEntries
import restarhalf.stellar.schedule.ui.navigation.graph.peEntries
import restarhalf.stellar.schedule.ui.navigation.rememberMainPagerState
import restarhalf.stellar.schedule.ui.screens.main.MainRouteContent
import restarhalf.stellar.schedule.ui.screens.main.MainScreenBackHandler
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.time.Clock


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppContent(
    vm: AppViewModel,
    bgVm: BackgroundViewModel,
    appUpdate: AppUpdatePort,
    pictureSelectorHost: @Composable (
        show: Boolean,
        onDismissRequest: () -> Unit,
        onPicked: (String) -> Unit,
        outputWidthPx: Int?,
        outputHeightPx: Int?,
    ) -> Unit = { _, _, _, _, _ -> },
    pdfFilePickerHost: @Composable (
        multiple: Boolean,
        onPicked: (List<restarhalf.stellar.schedule.papers.PickedAttachment>) -> Unit,
    ) -> Unit = { _, _ -> },
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
    openUri: (String) -> Boolean = { false },
    showMessage: (String) -> Unit = {},
    canSaveAwardPicture: Boolean = false,
    saveAwardPicture: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    runSync: suspend () -> Unit,
    saveLog: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    saveCsv: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    canSaveImage: Boolean = false,
    saveImage: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
) {
    val appState = LocalAppState.current
    val appUiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    val backgroundUiState by bgVm.uiState.collectAsStateWithLifecycle()
    val surfaceColor = colors.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val pagerState = rememberPagerState(pageCount = { RootTabs.size })
    val mainPagerState = rememberMainPagerState(pagerState)
    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    val backStack = rememberNavBackStack<Screen>(Screen.Main)
    val navigator = remember(backStack) { AppNavigator(backStack) }
    val currentRoute = (navigator.current() as? Screen) ?: Screen.Main
    val chromeState =
        remember(currentRoute, mainPagerState.selectedPage, appState.barMode) {
            AppChromeState(
                currentScreen = mainPagerState.currentScreen,
                isMainRoute = currentRoute == Screen.Main,
                barMode = appState.barMode,
            )
        }
    val pictureSelectorHostState by rememberUpdatedState(pictureSelectorHost)
    val ensureNotificationPermissionState by rememberUpdatedState(ensureNotificationPermission)
    val openUriState by rememberUpdatedState(openUri)
    val showMessageState by rememberUpdatedState(showMessage)
    val saveAwardPictureState by rememberUpdatedState(saveAwardPicture)
    val saveImageState by rememberUpdatedState(saveImage)
    val canSaveImageState by rememberUpdatedState(canSaveImage)
    val runSyncState by rememberUpdatedState(runSync)
    val announcementHttpClient = koinInject<HttpClient>(named("announcement"))
    val saveImageFromUrl: suspend (String) -> Boolean = remember(
        announcementHttpClient,
        saveImageState,
    ) {
        { url ->
            withContext(AppIoDispatcher) {
                runCatching {
                    val bytes = announcementHttpClient.get(url).body<ByteArray>()
                    saveImageState(deriveImageFileName(url), bytes)
                }.getOrDefault(false)
            }
        }
    }
    val announcementVm: AnnouncementViewModel = koinViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                announcementVm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navCornerRadius = rememberNavSystemCornerRadius()
    val navEffects =
        remember(navCornerRadius) {
            NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = navCornerRadius,
                cornerClipMode = NavCornerClipMode.Leading,
                dimAmount = 0f,
                blockInputDuringTransition = true,
            )
        }

    val deps = AppNavDeps(
        navigator = navigator,
        appUpdate = appUpdate,
        bgVm = bgVm,
        announcementVm = announcementVm,
        totalWeeks = appUiState.totalWeeks,
        pictureSelectorHost = pictureSelectorHostState,
        pdfFilePickerHost = pdfFilePickerHost,
        ensureNotificationPermission = ensureNotificationPermissionState,
        openUri = openUriState,
        showMessage = showMessageState,
        canSaveAwardPicture = canSaveAwardPicture,
        saveAwardPicture = saveAwardPictureState,
        saveLog = saveLog,
        canSaveImage = canSaveImageState,
        saveImageFromUrl = saveImageFromUrl,
        onLogoutJwxt = { vm.logout() },
    )

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalAppChromeState provides chromeState,
        LocalMainPagerState provides mainPagerState,
    ) {
        MainScreenBackHandler(mainPagerState, navigator)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.surface),
        ) {
            Scaffold(
                containerColor = colors.background.copy(alpha = 0f),
                bottomBar = {
                    AppBottomBar(backdrop = backdrop)
                },
            ) { innerPadding ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                            .background(colors.surface),
                ) {
                    backgroundUiState.backgroundImageUri?.let { uri ->
                        AsyncImage(
                            model = toAsyncImageModel(uri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .blur(25.dp * backgroundUiState.backgroundBlur)
                                    .alpha(backgroundUiState.backgroundAlpha),
                        )
                    }

                    AppScaffoldBody(
                        innerPadding = innerPadding,
                        componentsAlpha = backgroundUiState.componentsAlpha,
                    ) {
                        NavDisplay(
                            backStack = backStack,
                            onBack = { navigator.pop() },
                            transition = TransparentStackTransition,
                            effects = navEffects,
                        ) {
                            entry<Screen.Main> {
                                MainRouteContent(
                                    bgVm = bgVm,
                                    vm = vm,
                                    announcementVm = announcementVm,
                                    runSync = runSyncState,
                                    ensureNotificationPermission = ensureNotificationPermissionState,
                                    saveCsv = saveCsv,
                                    showMessage = showMessage,
                                    canSaveImage = canSaveImageState,
                                    saveImage = saveImageState,
                                )
                            }
                            peEntries(deps)
                            papersEntries(deps)
                            evaluationEntries(deps)
                            courseSelectionEntries(deps)
                            announcementEntries(deps)
                            miscEntries(deps)
                        }
                    }
                }
            }
        }
    }
}


private fun deriveImageFileName(url: String): String {
    val tail = url.substringAfterLast('/', "")
        .substringBefore('?')
        .substringBefore('#')
        .takeIf { it.isNotBlank() && !it.contains('/') && it.length <= 120 }
        ?: return "chrnova_${Clock.System.now().toEpochMilliseconds()}.jpg"
    return if (tail.contains('.')) tail else "$tail.jpg"
}
