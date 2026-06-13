package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * 应用Scaffold内边距的CompositionLocal
 * 
 * 用于在组件树中提供Scaffold的内边距，避免重复传递padding参数。
 */
val LocalAppScaffoldPadding = staticCompositionLocalOf { PaddingValues(0.dp) }
