package restarhalf.stellar.schedule

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf

@Stable
data class AppState(
    val themeMode: Int = 0,
    val barMode: Int = 0,
)

val LocalAppState = compositionLocalOf<AppState> {
    error("No AppState provided!")
}
