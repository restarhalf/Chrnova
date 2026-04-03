package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.components.LocalComponentsAlpha
import restarhalf.stellar.schedule.ui.icons.Examination
import restarhalf.stellar.schedule.ui.icons.Grade
import restarhalf.stellar.schedule.ui.icons.Home
import restarhalf.stellar.schedule.ui.icons.Schedule
import restarhalf.stellar.schedule.ui.icons.Settings
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.theme.LocalDismissState
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
    shellState: AppShellState,
    onSwitchTab: (Screen) -> Unit,
    backdrop: Backdrop
) {
    AnimatedVisibility(
        visible = !shellState.isWideScreen && shellState.showBottomBar,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(200)),
        exit =
            slideOutVertically(targetOffsetY = { it }) +
                    fadeOut(animationSpec = tween(200)),
    ) {
        when (shellState.barMode) {
            0 -> {
                NavigationBar {
                    appTabSpecs.forEach { tab ->
                        NavigationBarItem(
                            selected = shellState.currentScreen == tab.screen,
                            onClick = { onSwitchTab(tab.screen) },
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
                            selected = shellState.currentScreen == tab.screen,
                            onClick = { onSwitchTab(tab.screen) },
                            icon = tab.icon,
                            label = tab.label,
                        )
                    }
                }
            }
            else -> {
                GlassNavigationBar(backdrop = backdrop) {
                    appTabSpecs.forEach { tab ->
                        GlassNavigationBarItem(
                            selected = shellState.currentScreen == tab.screen,
                            onClick = { onSwitchTab(tab.screen) },
                            icon = tab.icon,
                            label = tab.label,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    currentScreen: Screen?,
    onSwitchTab: (Screen) -> Unit,
) {
    NavigationRail {
        appTabSpecs.forEach { tab ->
            NavigationRailItem(
                selected = currentScreen == tab.screen,
                onClick = { onSwitchTab(tab.screen) },
                icon = tab.icon,
                label = tab.label,
            )
        }
    }
}

@Composable
fun FirstOpenNoticeDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onExit: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = "温馨提示",
        summary = "此应用并非大连民族大学官方应用，仅为自娱自乐",
        onDismissRequest = onDismiss,
        content = {
            val dismissState = LocalDismissState.current
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        dismissState?.invoke()
                        onExit()
                    },
                ) {
                    Text(text = "退出应用")
                }

                Spacer(modifier = Modifier.size(16.dp))

                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = { dismissState?.invoke() },
                ) {
                    Text(text = "我已知晓", color = MiuixTheme.colorScheme.onPrimary)
                }
            }
        },
    )
}

@Composable
fun AppScaffoldBody(
    shellState: AppShellState,
    innerPadding: PaddingValues,
    componentsAlpha: Float,
    onSwitchTab: (Screen) -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalComponentsAlpha provides componentsAlpha,
        LocalAppScaffoldPadding provides innerPadding,
        LocalIsWideScreen provides shellState.isWideScreen,
    ) {
        if (shellState.isWideScreen && shellState.showBottomBar) {
            val layoutDirection = LocalLayoutDirection.current
            val startPadding = innerPadding.calculateStartPadding(layoutDirection)
            val endPadding = innerPadding.calculateEndPadding(layoutDirection)
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = startPadding, end = endPadding),
            ) {
                AppNavigationRail(
                    currentScreen = shellState.currentScreen,
                    onSwitchTab = onSwitchTab,
                )

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
