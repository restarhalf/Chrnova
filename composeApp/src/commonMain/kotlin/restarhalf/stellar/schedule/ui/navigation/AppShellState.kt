package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.domain.model.SettingsKeys

@Stable
data class AppShellState(
    val currentScreen: Screen?,
    val showBottomBar: Boolean,
    val isWideScreen: Boolean,
    val barMode: Int,
)

@Composable
fun rememberAppShellState(
    currentScreen: Screen?,
    isWideScreen: Boolean,
    settings: ObservableSettings,
): AppShellState {
    var barMode by remember {
        mutableIntStateOf(settings.getInt(SettingsKeys.FLOATING_BAR, 0))
    }

    DisposableEffect(settings) {
        val listener = settings.addIntListener(SettingsKeys.FLOATING_BAR, 0) { newValue ->
            barMode = newValue
        }
        onDispose { listener.deactivate() }
    }

    val showBottomBar =
        currentScreen != null && isRootScreen(currentScreen)

    return AppShellState(
        currentScreen = currentScreen,
        showBottomBar = showBottomBar,
        isWideScreen = isWideScreen,
        barMode = barMode,
    )
}

private fun isRootScreen(screen: Screen): Boolean {
    return screen == Screen.Home ||
            screen == Screen.Schedule ||
            screen == Screen.Examination ||
            screen == Screen.Grade ||
            screen == Screen.Settings
}
