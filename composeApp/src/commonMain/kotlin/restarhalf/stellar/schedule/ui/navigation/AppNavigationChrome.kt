package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import restarhalf.stellar.schedule.ui.components.LocalComponentsAlpha
import restarhalf.stellar.schedule.ui.icons.Examination
import restarhalf.stellar.schedule.ui.icons.Home
import restarhalf.stellar.schedule.ui.icons.PE
import restarhalf.stellar.schedule.ui.icons.Schedule
import restarhalf.stellar.schedule.ui.icons.Settings
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    // 暗色模式用主题背景亮度判断，避免直接 isSystemInDarkTheme()
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    // blur 可用性：运行时 shader 支持 + backdrop 非空（与液态玻璃模式一致）
    val blurActive = isRuntimeShaderSupported() && backdrop != null

    // 带动画的显示/隐藏
    AnimatedVisibility(
        visible = chromeState.showNavigationChrome,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(200)),
        exit =
            slideOutVertically(targetOffsetY = { it }) +
                fadeOut(animationSpec = tween(200)),
    ) {
        when (chromeState.barMode) {
            // 固定导航栏模式：blur 激活时用 textureBlur 模糊背景，导航栏自身透明
            0 -> {
                val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
                Box(
                    modifier =
                        Modifier
                            .then(
                                if (blurActive) {
                                    Modifier.textureBlur(
                                        backdrop = backdrop,
                                        shape = RectangleShape,
                                        blurRadius = 25f,
                                        colors =
                                            BlurDefaults.blurColors(
                                                blendColors = listOf(
                                                    BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.8f)),
                                                ),
                                            ),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .background(barColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            ),
                ) {
                    NavigationBar(color = barColor) {
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
            }

            // 悬浮导航栏模式：textureBlur + 玻璃描边高光，对齐 example
            1 -> {
                val floatingBarColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
                val floatingBarShape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius)
                val floatingHighlight = remember(isDark) {
                    if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
                }
                FloatingNavigationBar(
                    modifier =
                        if (blurActive) {
                            Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = floatingBarShape,
                                blurRadius = 25f,
                                colors =
                                    BlurDefaults.blurColors(
                                        blendColors = listOf(
                                            BlendColorEntry(color = MiuixTheme.colorScheme.surfaceContainer.copy(0.6f)),
                                        ),
                                    ),
                                highlight = floatingHighlight,
                            )
                        } else {
                            Modifier
                        },
                    color = floatingBarColor,
                ) {
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
                    isBlurActive = blurActive,
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
