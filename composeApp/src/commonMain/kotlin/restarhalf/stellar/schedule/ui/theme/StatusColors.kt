package restarhalf.stellar.schedule.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import top.yukonga.miuix.kmp.theme.MiuixTheme

object StatusColors {

    private val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = MiuixTheme.colorScheme.background.luminance() < 0.5f

    val danger: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFEF9A9A) else Color(0xFFE53935)

    val warning: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800)

    val healthy: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)

    val neutral: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFBDBDBD) else Color(0xFF9E9E9E)

    val labTag: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFFFD08A) else Color(0xFFFCB065)

    val transTag: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFFFA8A8) else Color(0xFFE54E4E)

    val mutedBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)

    val mutedTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFBDBDBD) else Color(0xFF6B6B6B)

    val mutedSub: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFF9E9E9E) else Color(0xFF6E6E6E)

    private const val STATUS_LUMINANCE_THRESHOLD = 0.45f

    fun onStatusOf(background: Color): Color =
        if (background.luminance() < STATUS_LUMINANCE_THRESHOLD) Color.White
        else Color(0xFF1F1F1F)
}
