package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.koin.compose.koinInject
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.getPlatform
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.screen.about.AwardDialog
import restarhalf.stellar.schedule.ui.components.screen.about.AwardPictureDialog
import restarhalf.stellar.schedule.ui.components.screen.about.DetailHeader
import restarhalf.stellar.schedule.ui.components.screen.about.JwxtWebDialog
import restarhalf.stellar.schedule.ui.components.screen.about.UpdateConfirmDialog
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.viewmodel.AboutUiEvent
import restarhalf.stellar.schedule.ui.viewmodel.AboutViewModel
import schedulekmp.composeapp.generated.resources.Res
import schedulekmp.composeapp.generated.resources.alipay
import schedulekmp.composeapp.generated.resources.wxpay
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onHandleEvent: (AboutUiEvent) -> Unit = {},
    onStartDownload: (AppUpdateInfo) -> Unit = {},
    appIcon: ImageBitmap? = null,
    showMessage: (String) -> Unit = {},
    canSaveAwardPicture: Boolean = false,
    onSaveAwardPicture: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
) {
    val vm: AboutViewModel = koinViewModel()
    val appInfo: AppInfoPort = koinInject()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val scope = rememberCoroutineScope()
    val showUpdateConfirm = remember { mutableStateOf(false) }
    val showAward = remember { mutableStateOf(false) }
    val showJwxt = remember { mutableStateOf(false) }
    val awardImageTitle = remember { mutableStateOf<String?>(null) }
    val awardImage = remember { mutableStateOf<DrawableResource?>(null) }
    val awardImagePath = remember { mutableStateOf<String?>(null) }
    val isInPreview = LocalInspectionMode.current
    val isAndroidPlatform = remember { getPlatform().name.startsWith("Android") }
    val handleEvent by rememberUpdatedState(onHandleEvent)

    val pendingUpdate by vm.pendingUpdate.collectAsState()
    val updateChecking by vm.updateChecking.collectAsState()
    val updateSummary by vm.updateSummary.collectAsState()

    val appName = remember(appInfo.appName) { appInfo.appName.ifBlank { "Schedule" } }
    val versionName = appInfo.versionName

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            handleEvent(event)
        }
    }

    LaunchedEffect(pendingUpdate) {
        if (pendingUpdate != null) {
            showUpdateConfirm.value = true
        }
    }

    val screenUi =
        remember(isInPreview, versionName, updateChecking, updateSummary) {
            vm.buildScreenUi(
                isInPreview = isInPreview,
                versionName = versionName,
                updateChecking = updateChecking,
                updateSummary = updateSummary,
            )
        }

    Scaffold(
        topBar = {
            AppPageTopBar(
                title = "关于",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        containerColor = Color.Transparent,
        popupHost = {
            if (showUpdateConfirm.value && pendingUpdate != null) {
                UpdateConfirmDialog(
                    show = showUpdateConfirm,
                    pendingUpdate = pendingUpdate,
                    onStartDownload = { info ->
                        onStartDownload(info)
                        vm.clearPendingUpdate()
                    },
                    onLater = { vm.clearPendingUpdate() },
                )
            }
            if (showAward.value) {
                AwardDialog(
                    show = showAward,
                    onWxpay = {
                        if (isAndroidPlatform) {
                            vm.requestWxPayAward()
                        } else {
                            awardImageTitle.value = "微信赞赏码"
                            awardImage.value = Res.drawable.wxpay
                            awardImagePath.value = "drawable/wxpay.webp"
                        }
                        showAward.value = false
                    },
                    onAlipay = {
                        if (isAndroidPlatform) {
                            vm.requestOpenAlipayAward()
                        } else {
                            awardImageTitle.value = "支付宝赞赏码"
                            awardImage.value = Res.drawable.alipay
                            awardImagePath.value = "drawable/alipay.webp"
                        }
                        showAward.value = false
                    },
                )
            }
            awardImage.value?.let { image ->
                AwardPictureDialog(
                    show = true,
                    title = awardImageTitle.value ?: "赞赏码",
                    image = image,
                    onDismissRequest = {
                        awardImageTitle.value = null
                        awardImage.value = null
                        awardImagePath.value = null
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
                                        val bytes = runCatching { Res.readBytes(path) }.getOrNull()
                                        if (bytes == null) {
                                            showMessage("读取图片失败，请重试")
                                            return@launch
                                        }
                                        val saved =
                                            runCatching {
                                                onSaveAwardPicture(
                                                    fileName,
                                                    bytes
                                                )
                                            }.getOrDefault(false)
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
            if (showJwxt.value) {
                JwxtWebDialog(
                    show = showJwxt,
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
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding =
                appPageContentPadding(
                    innerPadding = paddingValues,
                    outerPadding = appScaffoldPadding,
                    extraTop = 12.dp,
                    extraStart =
                        16.dp +
                                WindowInsets.displayCutout
                                    .asPaddingValues()
                                    .calculateLeftPadding(LayoutDirection.Ltr),
                    extraEnd =
                        16.dp +
                                WindowInsets.displayCutout
                                    .asPaddingValues()
                                    .calculateRightPadding(LayoutDirection.Ltr),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            overscrollEffect = null,
        ) {
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item {
                DetailHeader(
                    appIcon = appIcon,
                    appName = appName,
                    version = screenUi.versionDisplay
                )
            }
            item {
                HorizontalDivider(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                )
            }
            item { Spacer(modifier = Modifier.height(6.dp)) }
            item {
                AppCard {
                    SuperArrow(
                        title = "检查更新",
                        summary = screenUi.updateActionSummary,
                        onClick = {
                            if (!screenUi.canCheckUpdate) return@SuperArrow
                            vm.checkUpdate(currentVersionName = screenUi.currentVersionForCheck)
                        },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(6.dp)) }
            item {
                AppCard {
                    SuperArrow(
                        title = "加入 QQ 群",
                        summary = "加入 QQ 群反馈 bug",
                        onClick = { vm.requestJoinDefaultQqGroup() },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(6.dp)) }
            item {
                AppCard {
                    SuperArrow(
                        title = "打开教务系统",
                        summary = "按需跳转到移动端或 PC 端",
                        onClick = { showJwxt.value = true },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(6.dp)) }
            item {
                AppCard {
                    SuperArrow(
                        title = "查看Github仓库",
                        summary = "给我点个星吧求求你们了",
                        onClick = { vm.requestOpenGithub() }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(6.dp)) }
            item {
                AppCard {
                    SuperArrow(
                        title = "进入发布页",
                        summary = "快安利给其他人吧",
                        onClick = { vm.requestPublishPage() }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(6.dp)) }
            item {
                AppCard {
                    SuperArrow(
                        title = "赞赏作者",
                        summary = "赞赏以支持继续更新",
                        onClick = { showAward.value = true },
                    )
                }
            }
        }
    }
}

