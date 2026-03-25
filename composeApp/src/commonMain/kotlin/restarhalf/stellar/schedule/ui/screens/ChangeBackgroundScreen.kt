package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.TitleSlider
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ChangeBackgroundViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChangeBackgroundScreen(
    onBack: () -> Unit,
    pictureSelectorHost: @Composable (show: Boolean, onDismissRequest: () -> Unit, onPicked: (String) -> Unit) -> Unit = { _, _, _ -> },
) {
    val vm: BackgroundViewModel = koinViewModel()
    val previewVm: ChangeBackgroundViewModel = koinViewModel()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val showPictureSelector = remember { mutableStateOf(false) }

    val backgroundImageUri by vm.backgroundImageUri.collectAsState()
    val backgroundAlpha by vm.backgroundAlpha.collectAsState()
    val backgroundBlur by vm.backgroundBlur.collectAsState()
    val componentsAlpha by vm.componentsAlpha.collectAsState()

    val screenUi =
        remember(backgroundImageUri, backgroundAlpha, backgroundBlur, componentsAlpha) {
            previewVm.buildScreenUi(
                backgroundImageUri = backgroundImageUri,
                backgroundAlpha = backgroundAlpha,
                backgroundBlur = backgroundBlur,
                componentsAlpha = componentsAlpha
            )
        }


    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "更换背景",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                })
        }) { paddingValues ->
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
                    extraStart = 16.dp,
                    extraEnd = 16.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            overscrollEffect = null
        ) {

            item { SmallTitle(text = "背景图片") }
            item {
                AppCard(modifier = Modifier.animateItem()) {
                    BasicComponent(
                        title = "选择图片",
                        summary = screenUi.imageSummary,
                        onClick = { showPictureSelector.value = true })
                    if (screenUi.hasCustomImage) {
                        BasicComponent(
                            title = "清除图片",
                            summary = "恢复为纯色背景",
                            onClick = { vm.setBackgroundImageUri(null) })
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }


            item { SmallTitle(text = "效果调节") }
            item {
                AppCard(modifier = Modifier.animateItem()) {
                    TitleSlider(
                        title = "背景透明度",
                        summary = screenUi.backgroundAlphaPercent,
                        value = backgroundAlpha,
                        onValueChange = { vm.setBackgroundAlpha(it) })
                    TitleSlider(
                        title = "背景模糊度",
                        summary = screenUi.backgroundBlurPercent,
                        value = backgroundBlur,
                        onValueChange = { vm.setBackgroundBlur(it) })
                    TitleSlider(
                        title = "组件透明度",
                        summary = screenUi.componentsAlphaPercent,
                        value = componentsAlpha,
                        onValueChange = { vm.setComponentsAlpha(it) })
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    pictureSelectorHost(
        showPictureSelector.value,
        { showPictureSelector.value = false },
        { croppedUri ->
            vm.setBackgroundImageUri(croppedUri)
            showPictureSelector.value = false
        },
    )
}
