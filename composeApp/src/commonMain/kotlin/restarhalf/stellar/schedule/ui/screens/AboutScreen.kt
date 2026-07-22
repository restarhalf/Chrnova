package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.DrawableResource
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.ui.blur.BlurredBar
import restarhalf.stellar.schedule.ui.blur.ColorBlendToken
import restarhalf.stellar.schedule.ui.blur.rememberBlurBackdrop
import restarhalf.stellar.schedule.ui.blur.rememberBlurEnabled
import restarhalf.stellar.schedule.ui.components.screen.about.AwardDialog
import restarhalf.stellar.schedule.ui.components.screen.about.AwardPictureDialog
import restarhalf.stellar.schedule.ui.components.screen.about.JwxtWebDialog
import restarhalf.stellar.schedule.ui.components.screen.about.UpdateConfirmDialog
import restarhalf.stellar.schedule.ui.effect.BgEffectBackground
import restarhalf.stellar.schedule.ui.icons.AppIcon
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.viewmodel.AboutUiEvent
import restarhalf.stellar.schedule.ui.viewmodel.AboutViewModel
import chrnova.composeapp.generated.resources.Res
import chrnova.composeapp.generated.resources.alipay
import chrnova.composeapp.generated.resources.wxpay
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.Platform
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.platform
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AboutScreen(
    vm: AboutViewModel,
    onBack: () -> Unit,
    onHandleEvent: (AboutUiEvent) -> Unit = {},
    onStartDownload: (AppUpdateInfo) -> Unit = {},
    showMessage: (String) -> Unit = {},
    canSaveAwardPicture: Boolean = false,
    onSaveAwardPicture: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    onIconTap: () -> Unit = {},
) {
    val appInfo: AppInfoPort = koinInject()
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showUpdateConfirm = remember { mutableStateOf(false) }
    val showAward = remember { mutableStateOf(false) }
    val showAwardPicture = remember { mutableStateOf(false) }
    val showJwxt = remember { mutableStateOf(false) }
    val awardImageTitle = remember { mutableStateOf<String?>(null) }
    val awardImage = remember { mutableStateOf<DrawableResource?>(null) }
    val awardImagePath = remember { mutableStateOf<String?>(null) }
    val handleEvent by rememberUpdatedState(onHandleEvent)
    val aboutUiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            handleEvent(event)
        }
    }

    LaunchedEffect(aboutUiState.pendingUpdate) {
        if (aboutUiState.pendingUpdate != null) {
            showUpdateConfirm.value = true
        }
    }

    val screenUi = remember(aboutUiState.updateChecking, aboutUiState.updateSummary) {
        vm.buildScreenUi(
            isInPreview = false,
            versionName = appInfo.versionName,
            updateChecking = aboutUiState.updateChecking,
            updateSummary = aboutUiState.updateSummary,
        )
    }

    val scrollProgress by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null && scrollProgress == 1f
    val barColor = if (blurActive) {
        Color.Transparent
    } else {
        if (scrollProgress == 1f) colors.surface else Color.Transparent
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AppPageTopBar(
                    title = "关于",
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    titleColor = colors.onSurface.copy(
                        alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Back,
                                contentDescription = "返回",
                                tint = colors.onSurface,
                            )
                        }
                    },
                )
            }
        },
        containerColor = Color.Transparent,
        popupHost = {
            if (showUpdateConfirm.value && aboutUiState.pendingUpdate != null) {
                UpdateConfirmDialog(
                    show = showUpdateConfirm.value,
                    onDismissRequest = { showUpdateConfirm.value = false },
                    pendingUpdate = aboutUiState.pendingUpdate,
                    onStartDownload = { info ->
                        onStartDownload(info)
                        vm.clearPendingUpdate()
                    },
                    onLater = { vm.clearPendingUpdate() },
                )
            }
            if (showAward.value) {
                AwardDialog(
                    show = showAward.value,
                    onDismissRequest = { showAward.value = false },
                    onWxpay = {
                        if (platform() == Platform.Android) {
                            vm.requestWxPayAward()
                            showAward.value = false
                        } else {
                            awardImageTitle.value = "微信赞赏码"
                            awardImage.value = Res.drawable.wxpay
                            awardImagePath.value = "drawable/wxpay.webp"
                            showAward.value = false
                            showAwardPicture.value = true
                        }
                    },
                    onAlipay = {
                        if (platform() == Platform.Android) {
                            vm.requestOpenAlipayAward()
                            showAward.value = false
                        } else {
                            awardImageTitle.value = "支付宝赞赏码"
                            awardImage.value = Res.drawable.alipay
                            awardImagePath.value = "drawable/alipay.webp"
                            showAward.value = false
                            showAwardPicture.value = true
                        }
                    },
                )
            }
            if (showAwardPicture.value) {
                awardImage.value?.let { image ->
                    AwardPictureDialog(
                        show = showAwardPicture.value,
                        title = awardImageTitle.value ?: "赞赏码",
                        image = image,
                        onDismissRequest = {
                            awardImageTitle.value = null
                            awardImage.value = null
                            awardImagePath.value = null
                            showAwardPicture.value = false
                        },
                        onSavePicture =
                            if (canSaveAwardPicture) {
                                {
                                    val path = awardImagePath.value
                                    if (path == null) {
                                        showMessage("读取图片失败，请重试")
                                    } else {
                                        val fileName = path.substringAfterLast('/')
                                        scope.launch {
                                            val bytes =
                                                runCatching { Res.readBytes(path) }
                                                    .onFailure {
                                                        AppLogger.log("About", "读取奖励图片失败: path=$path", it)
                                                    }
                                                    .getOrNull()
                                            if (bytes == null) {
                                                showMessage("读取图片失败，请重试")
                                                return@launch
                                            }
                                            val saved =
                                                runCatching {
                                                    onSaveAwardPicture(fileName, bytes)
                                                }.onFailure {
                                                    AppLogger.log("About", "保存奖励图片失败: fileName=$fileName", it)
                                                }.getOrDefault(false)
                                            showAwardPicture.value = false
                                            showMessage(
                                                if (saved) "图片已保存" else "保存失败，请重试"
                                            )
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                    )
                }
            }
            if (showJwxt.value) {
                JwxtWebDialog(
                    show = showJwxt.value,
                    onDismissRequest = { showJwxt.value = false },
                    onPc = {
                        vm.requestOpenJwxtPc()
                        showJwxt.value = false
                    },
                    onMobile = {
                        vm.requestOpenJwxtMobile()
                        showJwxt.value = false
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            AboutContent(
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                lazyListState = lazyListState,
                scrollProgress = scrollProgress,
                appName = appInfo.appName,
                screenUi = screenUi,
                isDark = isDark,
                onIconTap = onIconTap,
                showAward = showAward,
                showJwxt = showJwxt,
                vm = vm,
            )
        }
    }
}

@Composable
private fun AboutContent(
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    lazyListState: LazyListState,
    scrollProgress: Float,
    appName: String,
    screenUi: AboutViewModel.AboutScreenUi,
    isDark: Boolean,
    onIconTap: () -> Unit,
    showAward: MutableState<Boolean>,
    showJwxt: MutableState<Boolean>,
    vm: AboutViewModel,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val colors = MiuixTheme.colorScheme
    val backdropLayer = rememberLayerBackdrop()

    val blurEnabled by rememberBlurEnabled()
    val effectBackground = remember(blurEnabled) { isRuntimeShaderSupported() && blurEnabled }

    val cardBlendColors = remember(isDark) {
        if (isDark) ColorBlendToken.Overlay_Thin_Light
        else ColorBlendToken.Pured_Regular_Light
    }
    val logoBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1.toInt()), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af500.toInt()), BlurBlendMode.Lab),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a.toInt()), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f.toInt()), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af200.toInt()), BlurBlendMode.Lab),
            )
        }
    }

    var logoHeightDp by remember { mutableStateOf(300.dp) }

    val versionCodeProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
    val projectNameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
    val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)

    val topPadding = innerPadding.calculateTopPadding()
    val startPadding = innerPadding.calculateStartPadding(layoutDirection)
    val endPadding = innerPadding.calculateEndPadding(layoutDirection)

    val logoPaddingTop = topPadding + 40.dp

    BgEffectBackground(
        dynamicBackground = effectBackground,
        modifier = Modifier.fillMaxSize(),
        bgModifier = Modifier.layerBackdrop(backdropLayer),
        isFullSize = true,
        effectBackground = effectBackground,
        alpha = { 1f - scrollProgress },
        isDarkTheme = isDark,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = logoPaddingTop + 52.dp,
                    start = startPadding,
                    end = endPadding,
                )
                .onSizeChanged { size ->
                    with(density) { logoHeightDp = size.height.toDp() }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        alpha = 1 - iconProgress
                        scaleX = 1 - (iconProgress * 0.05f)
                        scaleY = 1 - (iconProgress * 0.05f)
                    },
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (blurEnabled) {
                                Modifier.textureBlur(
                                    backdrop = backdropLayer,
                                    shape = RoundedCornerShape(0.dp),
                                    blurRadius = 150f,
                                    colors = BlurColors(blendColors = logoBlend),
                                    contentBlendMode = BlendMode.DstIn,
                                    enabled = true,
                                )
                            } else Modifier
                        ),
                    painter = rememberVectorPainter(AppIcon),
                    colorFilter = ColorFilter.tint(colors.onBackground),
                    contentDescription = "icon",
                )
            }
            Text(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 5.dp)
                    .graphicsLayer {
                        alpha = 1 - projectNameProgress
                        scaleX = 1 - (projectNameProgress * 0.05f)
                        scaleY = 1 - (projectNameProgress * 0.05f)
                    }
                    .then(
                        if (blurEnabled) {
                            Modifier.textureBlur(
                                backdrop = backdropLayer,
                                shape = RoundedCornerShape(16.dp),
                                blurRadius = 150f,
                                colors = BlurColors(blendColors = logoBlend),
                                contentBlendMode = BlendMode.DstIn,
                                enabled = true,
                            )
                        } else Modifier
                    ),
                text = appName,
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = 1 - versionCodeProgress
                        scaleX = 1 - (versionCodeProgress * 0.05f)
                        scaleY = 1 - (versionCodeProgress * 0.05f)
                    },
                color = colors.onSurfaceVariantSummary,
                text = screenUi.versionDisplay,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = topPadding,
                start = startPadding,
                end = endPadding,
            ),
        ) {
            item(key = "logoSpacer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(
                            logoHeightDp + 52.dp + logoPaddingTop - topPadding + 126.dp,
                        ),
                    contentAlignment = Alignment.TopCenter,
                    content = { },
                )
            }

            item(key = "about") {
                Box {
                    Spacer(Modifier.fillParentMaxHeight())
                    Column(
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        SmallTitle(text = "应用信息")
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                                .then(
                                    if (blurEnabled) {
                                        Modifier.textureBlur(
                                            backdrop = backdropLayer,
                                            shape = RoundedCornerShape(16.dp),
                                            blurRadius = 60f,
                                            colors = BlurColors(blendColors = cardBlendColors),
                                            enabled = true,
                                        )
                                    } else Modifier
                                ),
                            colors = CardDefaults.defaultColors(
                                if (blurEnabled) Color.Transparent else colors.surfaceContainer,
                                Color.Transparent,
                            ),
                        ) {
                            var versionTapCount by remember { mutableStateOf(0) }
                            BasicComponent(
                                title = "应用版本",
                                summary = screenUi.versionDisplay,
                                onClick = {
                                    versionTapCount++
                                    if (versionTapCount >= 5) {
                                        versionTapCount = 0
                                        onIconTap()
                                    }
                                },
                            )
                            BasicComponent(
                                title = "检查更新",
                                summary = screenUi.updateActionSummary,
                                onClick = {
                                    if (!screenUi.canCheckUpdate) return@BasicComponent
                                    vm.checkUpdate(currentVersionName = screenUi.currentVersionForCheck)
                                },
                            )
                        }

                        SmallTitle(text = "功能")
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                                .then(
                                    if (blurEnabled) {
                                        Modifier.textureBlur(
                                            backdrop = backdropLayer,
                                            shape = RoundedCornerShape(16.dp),
                                            blurRadius = 60f,
                                            colors = BlurColors(blendColors = cardBlendColors),
                                            enabled = true,
                                        )
                                    } else Modifier
                                ),
                            colors = CardDefaults.defaultColors(
                                if (blurEnabled) Color.Transparent else colors.surfaceContainer,
                                Color.Transparent,
                            ),
                        ) {
                            ArrowPreference(
                                title = "加入 QQ 群",
                                summary = "加入 QQ 群反馈 bug",
                                onClick = { vm.requestJoinDefaultQqGroup() },
                            )
                            ArrowPreference(
                                title = "打开教务系统",
                                summary = "按需跳转到移动端或 PC 端",
                                onClick = { showJwxt.value = true },
                            )
                            ArrowPreference(
                                title = "赞赏作者",
                                summary = "赞赏以支持继续更新",
                                onClick = { showAward.value = true },
                            )
                        }

                        SmallTitle(text = "项目")
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                                .then(
                                    if (blurEnabled) {
                                        Modifier.textureBlur(
                                            backdrop = backdropLayer,
                                            shape = RoundedCornerShape(16.dp),
                                            blurRadius = 60f,
                                            colors = BlurColors(blendColors = cardBlendColors),
                                            enabled = true,
                                        )
                                    } else Modifier
                                ),
                            colors = CardDefaults.defaultColors(
                                if (blurEnabled) Color.Transparent else colors.surfaceContainer,
                                Color.Transparent,
                            ),
                        ) {
                            ArrowPreference(
                                title = "Chrnova",
                                summary = "github.com/restarhalf/Chrnova",
                                onClick = { vm.requestOpenGithub() },
                            )
                        }

                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
                    }
                }
            }
        }
    }
}
