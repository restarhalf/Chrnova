package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
data class AppChromeState(
    val currentScreen: Screen,
    val isMainRoute: Boolean,
    val barMode: Int,
) {
    val showNavigationChrome: Boolean
        get() = isMainRoute
}

val LocalAppChromeState = staticCompositionLocalOf<AppChromeState> {
    error("No AppChromeState provided!")
}

val LocalNavigator = staticCompositionLocalOf<AppNavigator> {
    error("No AppNavigator provided!")
}
