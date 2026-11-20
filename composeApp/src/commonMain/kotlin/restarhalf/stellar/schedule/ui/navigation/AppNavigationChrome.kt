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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.haooz.softglass.backdrop.GlassBackdrop
import restarhalf.stellar.schedule.ui.chrome.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.components.LocalComponentsAlpha
import restarhalf.stellar.schedule.ui.components.LocalLiquidGlassBackdrop
import restarhalf.stellar.schedule.ui.components.LocalWallpaperBackdrop
import restarhalf.stellar.schedule.ui.components.bottombar.LiquidBottomBarFocusState
import restarhalf.stellar.schedule.ui.components.bottombar.LiquidBottomTrailing
import restarhalf.stellar.schedule.ui.icons.Examination
import restarhalf.stellar.schedule.ui.icons.Home
import restarhalf.stellar.schedule.ui.icons.PE
import restarhalf.stellar.schedule.ui.icons.Schedule
import restarhalf.stellar.schedule.ui.icons.Settings
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
    glassBackdrop: GlassBackdrop,
    focusState: LiquidBottomBarFocusState? = null,
) {
    val chromeState = LocalAppChromeState.current
    val mainPagerState = LocalMainPagerState.current
    val navigator = LocalNavigator.current
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f

    val primaryTabs = appTabSpecs.dropLast(1)
    val settingsSpec = appTabSpecs.last()
    val selectedIndex =
        primaryTabs.indexOfFirst { it.screen == chromeState.currentScreen }.coerceAtLeast(0)

    AnimatedVisibility(
        visible = chromeState.showNavigationChrome,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(200)),
        exit =
            slideOutVertically(targetOffsetY = { it }) +
                fadeOut(animationSpec = tween(200)),
    ) {
        LiquidGlassNavigationBar(
            tabs = primaryTabs.map { tab -> LiquidBarTab(icon = tab.icon, label = tab.label) },
            selectedIndex = selectedIndex,
            onTabSelected = { index -> mainPagerState.animateTo(primaryTabs[index].screen) },
            backdrop = glassBackdrop,
            isExpanded = focusState?.isExpanded ?: true,
            onExpandedChange = focusState?.let { state -> state::updateExpanded },
            trailing =
                LiquidBottomTrailing(
                    icon = settingsSpec.icon,
                    contentDescription = settingsSpec.label,
                    onClick = { navigator.push(Screen.Settings) },
                    iconTint = liquidTrailingTint(isDark),
                ),
        )
    }
}

@Composable
fun AppScaffoldBody(
    innerPadding: PaddingValues,
    componentsAlpha: Float,
    wallpaperBackdrop: GlassBackdrop? = null,
    liquidGlassBackdrop: GlassBackdrop? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalComponentsAlpha provides componentsAlpha,
        LocalWallpaperBackdrop provides wallpaperBackdrop,
        LocalLiquidGlassBackdrop provides liquidGlassBackdrop,
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
