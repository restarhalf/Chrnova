package top.yukonga.miuix.kmp.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Android 侧：判 `screenWidthDp >= 600`。
 *
 * 用 `LocalConfiguration` 是为了跟 Android 原生口径一致 ——
 * 分屏、折叠屏、多窗口下它的 dp 宽度与其他方案不同。
 */
@Composable
actual fun isTabletWidth(): Boolean =
    LocalConfiguration.current.screenWidthDp >= 600