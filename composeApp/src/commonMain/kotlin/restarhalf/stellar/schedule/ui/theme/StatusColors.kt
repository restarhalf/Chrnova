package restarhalf.stellar.schedule.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 语义色 token。集中管理状态、标签、muted 色谱，
 * 调用方一律走 token，避免散落的 Color(0xFF...)。
 */
object StatusColors {

    private val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = MiuixTheme.colorScheme.background.luminance() < 0.5f

    /** 红：失败 / 错误 / 危险 */
    val danger: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFEF9A9A) else Color(0xFFE53935)

    /** 黄/橙：警告 / 进行中 / 中等 */
    val warning: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800)

    /** 绿：成功 / 健康 / 通过 */
    val healthy: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)

    /** 灰：中性 / 未知 / 已结束 */
    val neutral: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFBDBDBD) else Color(0xFF9E9E9E)

    /** 实验课标签色 */
    val labTag: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFFFD08A) else Color(0xFFFCB065)

    /** 调课标签色 */
    val transTag: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFFFA8A8) else Color(0xFFE54E4E)

    /** 非本周课程 muted 背景 */
    val mutedBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)

    /** 非本周课程 muted 标题（浅色下与 mutedBackground 对比度 ≥ 4.5:1） */
    val mutedTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFBDBDBD) else Color(0xFF6B6B6B)

    /** 非本周课程 muted 副标题（浅色下与 mutedBackground 对比度 ≥ 4.5:1） */
    val mutedSub: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFF9E9E9E) else Color(0xFF6E6E6E)

    private const val STATUS_LUMINANCE_THRESHOLD = 0.45f

    /**
     * 状态色之上的可读前景色。
     *
     * 深色模式下的语义色（healthy/neutral 等）被提亮为浅色底，
     * 白字对比度会跌破 2:1，此时改用深字；浅色模式保持原有白字观感。
     */
    fun onStatusOf(background: Color): Color =
        if (background.luminance() < STATUS_LUMINANCE_THRESHOLD) Color.White
        else Color(0xFF1F1F1F)
}
