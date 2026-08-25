package restarhalf.stellar.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
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
import restarhalf.stellar.schedule.ui.navigation.MainPagerState
import restarhalf.stellar.schedule.ui.navigation.RootTabs
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.navigation.TransparentStackTransition
import restarhalf.stellar.schedule.ui.navigation.rememberMainPagerState
import restarhalf.stellar.schedule.ui.navigation.rootTabAt
import restarhalf.stellar.schedule.ui.screens.AboutScreen
import restarhalf.stellar.schedule.ui.screens.ChangeBackgroundScreen
import restarhalf.stellar.schedule.ui.screens.CourseEditScreen
import restarhalf.stellar.schedule.ui.screens.EMSScreen
import restarhalf.stellar.schedule.ui.screens.ElectiveCreditScreen
import restarhalf.stellar.schedule.ui.screens.ExamEditScreen
import restarhalf.stellar.schedule.ui.screens.HomeScreen
import restarhalf.stellar.schedule.ui.screens.JwxtLoginScreen
import restarhalf.stellar.schedule.ui.screens.LogScreen
import restarhalf.stellar.schedule.ui.screens.ProfileScreen
import restarhalf.stellar.schedule.ui.screens.ScheduleScreen
import restarhalf.stellar.schedule.ui.screens.SettingsScreen
import restarhalf.stellar.schedule.ui.screens.announcement.AnnouncementDetailScreen
import restarhalf.stellar.schedule.ui.screens.announcement.AnnouncementImageViewerScreen
import restarhalf.stellar.schedule.ui.screens.announcement.AnnouncementListScreen
import restarhalf.stellar.schedule.ui.screens.courseselection.CourseSelectionScreen
import restarhalf.stellar.schedule.ui.screens.evaluation.EvaluationCourseScreen
import restarhalf.stellar.schedule.ui.screens.evaluation.EvaluationDetailScreen
import restarhalf.stellar.schedule.ui.screens.evaluation.EvaluationListScreen
import restarhalf.stellar.schedule.ui.screens.evaluation.EvaluationSubmitScreen
import restarhalf.stellar.schedule.ui.screens.foodroulette.FoodItem
import restarhalf.stellar.schedule.ui.screens.foodroulette.FoodQRCodeScreen
import restarhalf.stellar.schedule.ui.screens.foodroulette.FoodRouletteScreen
import restarhalf.stellar.schedule.ui.screens.papers.PapersDetailScreen
import restarhalf.stellar.schedule.ui.screens.papers.PapersListScreen
import restarhalf.stellar.schedule.ui.screens.papers.PapersUploadScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEDetailScreen
import restarhalf.stellar.schedule.ui.screens.pe.PELoginScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEQRCodeScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEScoreScreen
import restarhalf.stellar.schedule.ui.viewmodel.AboutViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ElectiveCreditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExamEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.HomeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.JwxtLoginViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PELoginViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PersonalInfoViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
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

/**
 * 应用主内容组件
 *
 * 负责渲染应用的主要UI结构，包括：
 * - 底部导航栏
 * - 页面导航和转场动画
 * - 背景图片显示
 * - 各功能页面的路由
 *
 * 使用 miuix-nav 实现页面导航：连续栈深度驱动（animatedTop），内置滑动转场、
 * 预测返回与边缘滑动返回。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppContent(
    /** 应用ViewModel，管理业务逻辑和状态 */
    vm: AppViewModel,
    /** 背景ViewModel，管理背景图片相关状态 */
    bgVm: BackgroundViewModel,
    /** 应用更新端口，处理版本检查和下载 */
    appUpdate: AppUpdatePort,
    /** 图片选择器宿主组件，用于处理图片选择回调 */
    pictureSelectorHost: @Composable (
        show: Boolean,
        onDismissRequest: () -> Unit,
        onPicked: (String) -> Unit,
        outputWidthPx: Int?,
        outputHeightPx: Int?,
    ) -> Unit = { _, _, _, _, _ -> },
    /** PDF文件选择器宿主组件 */
    pdfFilePickerHost: @Composable (
        onPicked: (ByteArray, String, String) -> Unit,
    ) -> Unit = {},
    /** 通知权限请求回调 */
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
    /** 打开URI的回调函数 */
    openUri: (String) -> Boolean = { false },
    /** 显示消息的回调函数（如Toast） */
    showMessage: (String) -> Unit = {},
    /** 是否可以保存奖励图片 */
    canSaveAwardPicture: Boolean = false,
    /** 保存奖励图片的挂起函数 */
    saveAwardPicture: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    /** 同步教务系统的挂起函数 */
    runSync: suspend () -> Unit,
    /** 保存日志文件的回调，返回保存路径或null */
    saveLog: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    /** 保存CSV文件的回调，返回保存路径或null */
    saveCsv: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    /** 是否允许把图片（如公告配图）保存到相册 */
    canSaveImage: Boolean = false,
    /** 保存图片字节到相册（fileName 为保存名），返回是否成功 */
    saveImage: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
) {
    val appState = LocalAppState.current
    val appUiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    val backgroundUiState by bgVm.uiState.collectAsStateWithLifecycle()
    val surfaceColor = colors.surface
    val backdrop = rememberLayerBackdrop{
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
    val scope = rememberCoroutineScope()
    val currentRoute = (navigator.current() as? Screen) ?: Screen.Main
    val chromeState =
        remember(
            currentRoute,
            mainPagerState.selectedPage,
            appState.barMode,
        ) {
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
                    val name = deriveImageFileName(url)
                    saveImageState(name, bytes)
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
        remember(
            appState.enableCornerClip,
            appState.blockInputDuringTransition,
            navCornerRadius,
        ) {
            NavDisplayEffects(
                enableCornerClip = appState.enableCornerClip,
                cornerClipRadius = navCornerRadius,
                cornerClipMode = NavCornerClipMode.Leading,
                dimAmount = 0f,
                blockInputDuringTransition = appState.blockInputDuringTransition,
            )
        }

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
                            // 透明页面 + 宿主背景图方案不能直接用 MiuixDefault：
                            // 其 covered 处理保持下层可见（静止时 alpha 0.9 + parallax），
                            // 一级页会从透明二级页里透出；TransparentStackTransition
                            // 让被覆盖层淡出到透明，scrim 转场中途峰值、静止归零。
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
                                )
                            }
                            entry<Screen.ChangeBackground> {
                                ChangeBackgroundScreen(
                                    vm = bgVm,
                                    onBack = { navigator.pop() },
                                    pictureSelectorHost = pictureSelectorHostState,
                                )
                            }
                            entry<Screen.About> {
                                val aboutVm: AboutViewModel = koinViewModel()
                                AboutScreen(
                                    vm = aboutVm,
                                    onBack = { navigator.pop() },
                                    showMessage = showMessageState,
                                    canSaveAwardPicture = canSaveAwardPicture,
                                    onSaveAwardPicture = saveAwardPictureState,
                                    onIconTap = { navigator.push(Screen.Log) },
                                    onHandleEvent = { event ->
                                        when (event) {
                                            is AboutViewModel.AboutUiEvent.OpenUri -> {
                                                if (!openUriState(event.uri)) {
                                                    showMessageState("无法打开链接")
                                                }
                                            }

                                            is AboutViewModel.AboutUiEvent.JoinQqGroup -> {
                                                if (!appUpdate.joinQqGroup(key = event.key)) {
                                                    showMessageState("请检查是否安装了 QQ")
                                                }
                                            }

                                            AboutViewModel.AboutUiEvent.WxPayAwardRequested -> {
                                                val saved = appUpdate.saveWxpayToPictures()
                                                showMessageState(
                                                    if (saved) {
                                                        "赞赏码已保存到相册，即将跳转到微信扫一扫"
                                                    } else {
                                                        "保存赞赏码失败，请重试"
                                                    },
                                                )
                                                if (!appUpdate.openWeChatScanDirect()) {
                                                    showMessageState("微信启动失败，请检查是否安装微信")
                                                }
                                            }
                                        }
                                    },
                                    onStartDownload = { info -> appUpdate.startDirectDownload(info) },
                                )
                            }
                            entry<Screen.ClassEdit> { screen ->
                                val isEdit = remember(screen.courseId) { screen.courseId != null }
                                val courseEditVm: CourseEditViewModel = koinViewModel()
                                CourseEditScreen(
                                    vm = courseEditVm,
                                    onBack = { navigator.pop() },
                                    isEdit = isEdit,
                                    onEditChanged = { },
                                    courseId = screen.courseId,
                                    initialDayOfWeek = screen.dayOfWeek,
                                    initialStartSection = screen.startSection,
                                    initialSelectedWeek = screen.selectedWeek, totalWeeks = appUiState.totalWeeks,
                                )
                            }
                            entry<Screen.ExamEdit> { screen ->
                                val isEdit = remember(screen.examinationId) { screen.examinationId != null }
                                val examEditVm: ExamEditViewModel = koinViewModel()
                                ExamEditScreen(
                                    vm = examEditVm,
                                    onBack = { navigator.pop() },
                                    isEdit = isEdit,
                                    onEditChanged = { },
                                    examinationId = screen.examinationId,
                                )
                            }
                            entry<Screen.Log> {
                                LogScreen(
                                    onBack = { navigator.pop() },
                                    onExport = { fileName, content ->
                                        scope.launch {
                                            val path = saveLog(fileName, content)
                                            showMessageState(if (path != null) "已保存: $path" else "保存失败")
                                        }
                                    },
                                )
                            }
                            entry<Screen.PEDetail> { screen ->
                                val peVm: PEViewModel = koinViewModel()
                                PEDetailScreen(
                                    vm = peVm,
                                    schoolYear = screen.schoolYear,
                                    onBack = { navigator.pop() }
                                )
                            }
                            entry<Screen.PEQRCode> {
                                val settingsVm: SettingsViewModel = koinViewModel()
                                val settingsUiState by settingsVm.uiState.collectAsStateWithLifecycle()
                                val peVm: PEViewModel = koinViewModel()
                                PEQRCodeScreen(
                                    vm = peVm,
                                    jwxtAuthProfile = settingsUiState.profile,
                                    onBack = { navigator.pop() },
                                )
                            }
                            entry<Screen.Profile> {
                                val settingsVm: SettingsViewModel = koinViewModel()
                                val settingsUiState by settingsVm.uiState.collectAsStateWithLifecycle()
                                val peVm: PEViewModel = koinViewModel()
                                val peUiState by peVm.uiState.collectAsStateWithLifecycle()
                                val personalInfoViewModel: PersonalInfoViewModel = koinViewModel()
                                ProfileScreen(
                                    peAuthProfile = peUiState.authProfile,
                                    jwxtAuthProfile = settingsUiState.profile,
                                    onBack = { navigator.pop() },
                                    onLogoutJW = { vm.logout() },
                                    onLogoutPE = { peVm.logout() },
                                    pictureSelectorHost = pictureSelectorHostState,
                                    personalInfoViewModel = personalInfoViewModel,
                                )
                            }
                            entry<Screen.Papers> {
                                val paperVm: PapersViewModel = koinViewModel()
                                PapersListScreen(
                                    vm = paperVm,
                                    onBack = { navigator.pop() },
                                    onPaperDetail = { paperId ->
                                        navigator.push(Screen.PapersDetail(paperId))
                                    },
                                    onUploadClick = { navigator.push(Screen.PapersUpload) },
                                )
                            }
                            entry<Screen.PapersDetail> { screen ->
                                val paperVm: PapersViewModel = koinViewModel()
                                PapersDetailScreen(
                                    vm = paperVm,
                                    paperId = screen.paperId,
                                    onBack = { navigator.pop() },
                                    onDownload = { url, title -> openUriState(url) },
                                )
                            }
                            entry<Screen.PapersUpload> {
                                val paperVm: PapersViewModel = koinViewModel()
                                PapersUploadScreen(
                                    vm = paperVm,
                                    onBack = { navigator.pop() },
                                    onResult = { showMessageState(it) },
                                    pdfFilePickerHost = pdfFilePickerHost,
                                )
                            }
                            entry<Screen.Evaluation> {
                                val evalVm: CourseEvaluationViewModel = koinViewModel()
                                EvaluationListScreen(
                                    vm = evalVm,
                                    onBack = { navigator.pop() },
                                    onCourseClick = { courseName, teacher ->
                                        navigator.push(Screen.EvaluationCourse(courseName, teacher))
                                    },
                                    onEvaluationDetail = { evaluationId ->
                                        navigator.push(Screen.EvaluationDetail(evaluationId))
                                    },
                                    onSubmitClick = { navigator.push(Screen.EvaluationSubmit()) },
                                )
                            }
                            entry<Screen.EvaluationCourse> { screen ->
                                val evalVm: CourseEvaluationViewModel = koinViewModel()
                                EvaluationCourseScreen(
                                    vm = evalVm,
                                    courseName = screen.courseName,
                                    teacher = screen.teacher,
                                    onBack = { navigator.pop() },
                                    onEvaluationDetail = { evaluationId ->
                                        navigator.push(Screen.EvaluationDetail(evaluationId))
                                    },
                                )
                            }
                            entry<Screen.EvaluationDetail> { screen ->
                                val evalVm: CourseEvaluationViewModel = koinViewModel()
                                EvaluationDetailScreen(
                                    vm = evalVm,
                                    evaluationId = screen.evaluationId,
                                    onBack = { navigator.pop() },
                                    onEditEvaluation = { evaluationId ->
                                        navigator.push(Screen.EvaluationSubmit(evaluationId))
                                    },
                                )
                            }
                            entry<Screen.EvaluationSubmit> { screen ->
                                val evalVm: CourseEvaluationViewModel = koinViewModel()
                                EvaluationSubmitScreen(
                                    vm = evalVm,
                                    onBack = { navigator.pop() },
                                    onSubmitted = { navigator.pop() },
                                    evaluationId = screen.evaluationId,
                                )
                            }
                            entry<Screen.JwxtLogin> {
                                val jwLoginVm: JwxtLoginViewModel = koinViewModel()
                                JwxtLoginScreen(
                                    vm = jwLoginVm,
                                    onBack = { navigator.pop() },
                                    onLoginSuccess = { navigator.pop() },
                                    inWel = false,
                                    next = {}
                                )
                            }
                            entry<Screen.PELogin> {
                                val peLoginVm: PELoginViewModel = koinViewModel()
                                PELoginScreen(
                                    vm = peLoginVm,
                                    onBack = { navigator.pop() },
                                    onLoginSuccess = { navigator.pop() },
                                    inWel = false,
                                    next = {}
                                )
                            }
                            entry<Screen.ElectiveCredit> {
                                val electiveCreditVm: ElectiveCreditViewModel = koinViewModel()
                                ElectiveCreditScreen(
                                    vm = electiveCreditVm,
                                    onBack = { navigator.pop() },
                                )
                            }
                            entry<Screen.FoodRoulette> {
                                FoodRouletteScreen(
                                    onBack = { navigator.pop() },
                                    onFoodSelected = { food ->
                                        navigator.push(
                                            Screen.FoodQRCode(
                                                foodName = food.name,
                                                qrContent = food.qrContent,
                                            )
                                        )
                                    },
                                )
                            }
                            entry<Screen.FoodQRCode> { screen ->
                                FoodQRCodeScreen(
                                    food = FoodItem(
                                        name = screen.foodName,
                                        qrContent = screen.qrContent,
                                    ),
                                    onBack = { navigator.pop() },
                                )
                            }
                            entry<Screen.CourseSelection> {
                                val courseSelectionVm: CourseSelectionViewModel = koinViewModel()
                                CourseSelectionScreen(
                                    vm = courseSelectionVm,
                                    onBack = { navigator.pop() },
                                    ensureNotificationPermission = ensureNotificationPermissionState,
                                )
                            }
                            entry<Screen.AnnouncementList> {
                                AnnouncementListScreen(
                                    vm = announcementVm,
                                    onBack = { navigator.pop() },
                                    onAnnouncementClick = { announcementId ->
                                        navigator.push(Screen.AnnouncementDetail(announcementId))
                                    },
                                )
                            }
                            entry<Screen.AnnouncementDetail> { screen ->
                                AnnouncementDetailScreen(
                                    vm = announcementVm,
                                    announcementId = screen.announcementId,
                                    onBack = { navigator.pop() },
                                    onImageClick = { url ->
                                        navigator.push(Screen.AnnouncementImageViewer(url))
                                    },
                                )
                            }
                            entry<Screen.AnnouncementImageViewer> { screen ->
                                AnnouncementImageViewerScreen(
                                    url = screen.url,
                                    alt = null,
                                    canSaveImage = canSaveImageState,
                                    saveImage = saveImageFromUrl,
                                    showMessage = showMessageState,
                                    onBack = { navigator.pop() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 主路由内容组件
 * 
 * 使用HorizontalPager实现底部导航栏的页面切换，支持左右滑动。
 * 包含以下页面：
 * - Home：首页，显示今日课程
 * - Schedule：课程表，按周查看完整课表
 * - EMS：考试与成绩
 * - PEScore：体育成绩
 * - Settings：设置
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainRouteContent(
    /** 背景ViewModel */
    bgVm: BackgroundViewModel,
    /** 应用ViewModel */
    vm: AppViewModel,
    /** 公告ViewModel（全局共享，首页红点与列表/详情页状态一致） */
    announcementVm: AnnouncementViewModel,
    /** 同步教务系统的挂起函数 */
    runSync: suspend () -> Unit,
    /** 通知权限请求回调 */
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit,
    /** 保存CSV文件的回调 */
    saveCsv: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    /** 显示消息的回调 */
    showMessage: (String) -> Unit = {},
) {
    val homeVm: HomeViewModel = koinViewModel()
    val scheduleVm: ScheduleViewModel = koinViewModel()
    val examVm: ExaminationViewModel = koinViewModel()
    val gradeVm: GradeViewModel = koinViewModel()
    val peVm: PEViewModel = koinViewModel()
    val settingsVm: SettingsViewModel = koinViewModel()
    val appState = LocalAppState.current
    val appUiState by vm.uiState.collectAsStateWithLifecycle()
    val syncUiState by vm.syncUiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val mainPagerState = LocalMainPagerState.current
    val courses by scheduleVm.allCourses.collectAsStateWithLifecycle()

    // 切回首页 tab 时刷新公告（首次组合 currentPage=0 也会触发一次），
    // 保证新发布的公告红点在首页及时出现
    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        if (rootTabAt(mainPagerState.pagerState.currentPage) == Screen.Home) {
            announcementVm.refresh()
        }
    }

    HorizontalPager(
        state = mainPagerState.pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = appState.enablePageUserScroll,
        verticalAlignment = Alignment.Top,
    ) { page ->
        when (rootTabAt(page)) {
            Screen.Home -> {
                val bgUiState by bgVm.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    vm = homeVm,
                    announcementVm = announcementVm,
                    onAnnouncementClick = { navigator.push(Screen.AnnouncementList) },
                    campus = appUiState.campus,
                    termStartMs = appUiState.termStartMs,
                    totalWeeks = appUiState.totalWeeks,
                    componentsAlpha = bgUiState.componentsAlpha,
                    hasBackground = bgUiState.backgroundImageUri != null
                )
            }

            Screen.Schedule -> {
                ScheduleScreen(
                    vm = scheduleVm,
                    onSync = runSync,
                    campus = appUiState.campus,
                    termStartMs = appUiState.termStartMs,
                    totalWeeks = appUiState.totalWeeks,
                    onAddLabCourse = { dayOfWeek, startSection, selectedWeek ->
                        navigator.push(Screen.ClassEdit(dayOfWeek = dayOfWeek, startSection = startSection, selectedWeek = selectedWeek))
                    },
                    onEditLabCourse = { courseId ->
                        navigator.push(Screen.ClassEdit(courseId = courseId))
                    },
                )
            }

            Screen.EMS -> {
                EMSScreen(
                    examVm = examVm,
                    gradeVm = gradeVm,
                    onLoadExaminations = { vm.fetchExaminationArrangements() },
                    onLoadGrades = { vm.fetchGradeReport() },
                    onAddExam = { navigator.push(Screen.ExamEdit()) },
                    onEditExam = { examId -> navigator.push(Screen.ExamEdit(examinationId = examId)) },
                    onNavigateToElectiveCredit = { navigator.push(Screen.ElectiveCredit) },
                )
            }

            Screen.PEScore -> {
                val settingsUiState by settingsVm.uiState.collectAsStateWithLifecycle()
                PEScoreScreen(
                    vm = peVm,
                    onNavigateToDetail = { schoolYear ->
                        navigator.push(Screen.PEDetail(schoolYear))
                    },
                    onQRCode = { navigator.push(Screen.PEQRCode) },
                    onLogin = { navigator.push(Screen.PELogin) },
                    jwxtAuthProfile = settingsUiState.profile
                )
            }

            Screen.Settings -> {
                val isPeLoggedIn by peVm.isLoggedIn.collectAsStateWithLifecycle()
                SettingsScreen(
                    vm = settingsVm,
                    syncUiState = syncUiState,
                    campus = appUiState.campus,
                    termStartMs = appUiState.termStartMs,
                    totalWeeks = appUiState.totalWeeks,
                    onSync = runSync,
                    onLogout = { vm.logout() },
                    ensureCourseReminderPermission = ensureNotificationPermission,
                    ensureExamReminderPermission = ensureNotificationPermission,
                    onCampusChange = { campus -> vm.onCampusChanged(campus) },
                    onTermStartChange = { termStartMs -> vm.onTermStartMsChanged(termStartMs) },
                    onTotalWeeksChange = { totalWeeks -> vm.onTotalWeeksChanged(totalWeeks) },
                    onChangeBackground = { navigator.push(Screen.ChangeBackground) },
                    onAbout = { navigator.push(Screen.About) },
                    onPaper = { navigator.push(Screen.Papers) },
                    onEvaluation = { navigator.push(Screen.Evaluation) },
                    onCourseSelection = { navigator.push(Screen.CourseSelection) },
                    onLogin = { navigator.push(Screen.JwxtLogin) },
                    onProfile = { navigator.push(Screen.Profile) },
                    onFoodRoulette = { navigator.push(Screen.FoodRoulette) },
                    isPeLoggedIn = isPeLoggedIn,
                    onExportCsv = saveCsv,
                    courses = courses,
                    showMessage = showMessage,
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navigator: AppNavigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navigator.current() is Screen.Main &&
                    navigator.backStackSize() == 1 &&
                    mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        },
    )
}

/** 从图片 URL 推导保存文件名，无合适末段时回退到带时间戳的默认名。 */
private fun deriveImageFileName(url: String): String {
    val tail = url.substringAfterLast('/', "")
        .substringBefore('?')
        .substringBefore('#')
        .takeIf { it.isNotBlank() && !it.contains('/') && it.length <= 120 }
        ?: return "chrnova_${Clock.System.now().toEpochMilliseconds()}.jpg"
    return if (tail.contains('.')) tail else "$tail.jpg"
}
