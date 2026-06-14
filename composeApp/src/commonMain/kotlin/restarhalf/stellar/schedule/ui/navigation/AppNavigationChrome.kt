package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import restarhalf.stellar.schedule.ui.components.LocalComponentsAlpha
import restarhalf.stellar.schedule.ui.icons.Examination
import restarhalf.stellar.schedule.ui.icons.Home
import restarhalf.stellar.schedule.ui.icons.PE
import restarhalf.stellar.schedule.ui.icons.Schedule
import restarhalf.stellar.schedule.ui.icons.Settings
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * 标签页规格数据类
 * 
 * @param screen 对应的Screen
 * @param icon 图标
 * @param label 标签文本
 */
private data class TabSpec(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
)

/** 底部导航栏标签页配置 */
private val appTabSpecs =
    listOf(
        TabSpec(screen = Screen.Home, icon = Home, label = "首页"),
        TabSpec(screen = Screen.Schedule, icon = Schedule, label = "课程表"),
        TabSpec(screen = Screen.EMS, icon = Examination, label = "考务"),
        TabSpec(screen = Screen.PEScore, icon = PE, label = "体测"),
        TabSpec(screen = Screen.Settings, icon = Settings, label = "设置"),
    )

/**
 * 应用底部导航栏
 * 
 * 根据barMode显示不同样式的导航栏：
 * - 0: 固定导航栏
 * - 1: 悬浮导航栏
 * - 2: 液态玻璃导航栏
 * 
 * @param backdrop 模糊背景层，用于液态玻璃效果
 */
@Composable
fun AppBottomBar(
    backdrop: LayerBackdrop?,
) {
    val chromeState = LocalAppChromeState.current
    val mainPagerState = LocalMainPagerState.current

    // 带动画的显示/隐藏
    AnimatedVisibility(
        visible = chromeState.showNavigationChrome,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(200)),
        exit =
            slideOutVertically(targetOffsetY = { it }) +
                fadeOut(animationSpec = tween(200)),
    ) {
        when (chromeState.barMode) {
            // 固定导航栏模式
            0 -> {
                NavigationBar {
                    appTabSpecs.forEach { tab ->
                        NavigationBarItem(
                            selected = chromeState.currentScreen == tab.screen,
                            onClick = { mainPagerState.animateTo(tab.screen) },
                            icon = tab.icon,
                            label = tab.label,
                        )
                    }
                }
            }

            // 悬浮导航栏模式
            1 -> {
                FloatingNavigationBar {
                    appTabSpecs.forEach { tab ->
                        FloatingNavigationBarItem(
                            selected = chromeState.currentScreen == tab.screen,
                            onClick = { mainPagerState.animateTo(tab.screen) },
                            icon = tab.icon,
                            label = tab.label,
                        )
                    }
                }
            }

            // 液态玻璃导航栏模式
            else -> {
                GlassNavigationBar(
                    items = appTabSpecs.map { tab ->
                        NavigationItem(
                            icon = tab.icon,
                            label = tab.label,
                        )
                    },
                    selectedIndex = appTabSpecs.indexOfFirst { it.screen == chromeState.currentScreen }.coerceAtLeast(0),
                    onItemClick = { index -> mainPagerState.animateTo(appTabSpecs[index].screen) },
                    backdrop = backdrop,
                    isBlurActive = true,
                )
            }
        }
    }
}

/**
 * 应用Scaffold内容区域
 * 
 * 提供组件透明度和Scaffold内边距的CompositionLocal。
 * 
 * @param innerPadding Scaffold内边距
 * @param componentsAlpha 组件透明度
 * @param content 内容Composable
 */
@Composable
fun AppScaffoldBody(
    innerPadding: PaddingValues,
    componentsAlpha: Float,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalComponentsAlpha provides componentsAlpha,
        LocalAppScaffoldPadding provides innerPadding,
    ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
            ) {
                content()
            }
        }
}
