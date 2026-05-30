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
import restarhalf.stellar.schedule.ui.icons.Grade
import restarhalf.stellar.schedule.ui.icons.Home
import restarhalf.stellar.schedule.ui.icons.Schedule
import restarhalf.stellar.schedule.ui.icons.Settings
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private data class TabSpec(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
)

private val appTabSpecs =
    listOf(
        TabSpec(screen = Screen.Home, icon = Home, label = "首页"),
        TabSpec(screen = Screen.Schedule, icon = Schedule, label = "课程表"),
        TabSpec(screen = Screen.Examination, icon = Examination, label = "考试"),
        TabSpec(screen = Screen.Grade, icon = Grade, label = "成绩"),
        TabSpec(screen = Screen.Settings, icon = Settings, label = "设置"),
    )

@Composable
fun AppBottomBar(
    backdrop: LayerBackdrop?,
) {
    val chromeState = LocalAppChromeState.current
    val mainPagerState = LocalMainPagerState.current

    AnimatedVisibility(
        visible = chromeState.showNavigationChrome,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(200)),
        exit =
            slideOutVertically(targetOffsetY = { it }) +
                fadeOut(animationSpec = tween(200)),
    ) {
        when (chromeState.barMode) {
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

            else -> {
                GlassNavigationBar(
                    items = appTabSpecs.map { tab ->
                        NavigationItem(
                            icon = tab.icon,
                            label = tab.label,
                        ) }
                    ,
                    selectedIndex = appTabSpecs.indexOfFirst { it.screen == chromeState.currentScreen }.coerceAtLeast(0),
                    onItemClick = { index -> mainPagerState.animateTo(appTabSpecs[index].screen) },
                    backdrop = backdrop,
                    isBlurActive = true,
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
