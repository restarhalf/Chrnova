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


private data class TabSpec(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
)


private val appTabSpecs =
    listOf(
        TabSpec(screen = Screen.Home, icon = Home, label = "首页"),
        TabSpec(screen = Screen.Schedule, icon = Schedule, label = "课程表"),
        TabSpec(screen = Screen.EMS, icon = Examination, label = "考务"),
        TabSpec(screen = Screen.PEScore, icon = PE, label = "体测"),
        TabSpec(screen = Screen.Settings, icon = Settings, label = "设置"),
    )


@Composable
fun AppBottomBar(
    backdrop: LayerBackdrop?,
) {
    val chromeState = LocalAppChromeState.current
    val mainPagerState = LocalMainPagerState.current

    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f

    val blurActive = isRuntimeShaderSupported() && backdrop != null


    AnimatedVisibility(
        visible = chromeState.showNavigationChrome,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(200)),
        exit =
            slideOutVertically(targetOffsetY = { it }) +
                fadeOut(animationSpec = tween(200)),
    ) {
        when (chromeState.barMode) {

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
