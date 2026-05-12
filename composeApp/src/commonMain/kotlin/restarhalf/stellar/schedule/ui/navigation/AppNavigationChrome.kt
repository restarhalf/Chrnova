package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import restarhalf.stellar.schedule.ui.components.LocalComponentsAlpha
import restarhalf.stellar.schedule.ui.icons.Examination
import restarhalf.stellar.schedule.ui.icons.Grade
import restarhalf.stellar.schedule.ui.icons.Home
import restarhalf.stellar.schedule.ui.icons.Schedule
import restarhalf.stellar.schedule.ui.icons.Settings
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Text
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
        TabSpec(screen = Screen.Examination, icon = Examination, label = "考试"),
        TabSpec(screen = Screen.Grade, icon = Grade, label = "成绩"),
        TabSpec(screen = Screen.Settings, icon = Settings, label = "设置"),
    )

@Composable
fun AppBottomBar(
    backdrop: Backdrop,
) {
    val chromeState = LocalAppChromeState.current
    val mainPagerState = LocalMainPagerState.current

    AnimatedVisibility(
        visible = !chromeState.isWideScreen && chromeState.showNavigationChrome,
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
                val layoutDirection = LocalLayoutDirection.current
                val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
                val horizontalInset =
                    maxOf(
                        navigationBarPadding.calculateStartPadding(layoutDirection),
                        navigationBarPadding.calculateEndPadding(layoutDirection),
                    )

                GlassNavigationBar(
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp + horizontalInset)
                            .padding(
                                bottom = 36.dp + navigationBarPadding.calculateBottomPadding(),
                            ),
                    selectedIndex =
                        appTabSpecs.indexOfFirst { it.screen == chromeState.currentScreen }
                            .takeIf { it >= 0 }
                            ?: 0,
                    onSelected = { index -> mainPagerState.animateTo(appTabSpecs[index].screen) },
                    backdrop = backdrop,
                    tabsCount = appTabSpecs.size,
                ) {
                    appTabSpecs.forEach { tab ->
                        GlassNavigationBarItem(
                            onClick = { mainPagerState.animateTo(tab.screen) },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigationRail() {
    val chromeState = LocalAppChromeState.current
    val mainPagerState = LocalMainPagerState.current

    NavigationRail {
        appTabSpecs.forEach { tab ->
            NavigationRailItem(
                selected = chromeState.currentScreen == tab.screen,
                onClick = { mainPagerState.animateTo(tab.screen) },
                icon = tab.icon,
                label = tab.label,
            )
        }
    }
}

@Composable
fun AppScaffoldBody(
    innerPadding: PaddingValues,
    componentsAlpha: Float,
    content: @Composable () -> Unit,
) {
    val chromeState = LocalAppChromeState.current

    CompositionLocalProvider(
        LocalComponentsAlpha provides componentsAlpha,
        LocalAppScaffoldPadding provides innerPadding,
        LocalIsWideScreen provides chromeState.isWideScreen,
    ) {
        if (chromeState.isWideScreen && chromeState.showNavigationChrome) {
            val layoutDirection = LocalLayoutDirection.current
            val startPadding = innerPadding.calculateStartPadding(layoutDirection)
            val endPadding = innerPadding.calculateEndPadding(layoutDirection)
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = startPadding, end = endPadding),
            ) {
                AppNavigationRail()

                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
