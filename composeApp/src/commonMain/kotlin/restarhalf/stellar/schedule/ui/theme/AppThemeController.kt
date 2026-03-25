package restarhalf.stellar.schedule.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun rememberAppThemeController(settings: ObservableSettings): ThemeController {
    var themeMode by remember(settings) {
        mutableIntStateOf(settings.getInt(SettingsKeys.THEME_MODE, 0))
    }

    DisposableEffect(settings) {
        val listener = settings.addIntListener(SettingsKeys.THEME_MODE, 0) { newValue ->
            themeMode = newValue
        }
        onDispose { listener.deactivate() }
    }

    return remember(themeMode) {
        ThemeController(
            colorSchemeMode = when (themeMode) {
                1 -> ColorSchemeMode.Light
                2 -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.System
            }
        )
    }
}