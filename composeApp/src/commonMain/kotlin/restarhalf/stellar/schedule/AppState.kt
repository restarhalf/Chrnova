package restarhalf.stellar.schedule

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 应用全局 UI 状态数据类
 *
 * 仅保存影响全局界面的偏好开关（对齐 miuix example 的定位）；
 * 业务数据（校区、学期时间、同步进度等）由 ViewModel 提供，不放这里。
 * 通过 CompositionLocal 提供给子组件使用，@Stable 确保 Compose 重组优化。
 */
@Stable
data class AppState(
    /** 主题模式：0=跟随系统，1=浅色，2=深色 */
    val themeMode: Int = 0,
    /** 浮动导航栏模式：0=标准模式，1=紧凑模式 */
    val barMode: Int = 0,
)

/**
 * 应用状态的CompositionLocal
 *
 * 用于在Compose组件树中提供AppState实例，子组件可通过LocalAppState.current访问。
 */
val LocalAppState = compositionLocalOf<AppState> {
    error("No AppState provided!")
}

/**
 * 应用状态更新函数的CompositionLocal
 *
 * 用于提供状态更新函数，子组件可通过LocalUpdateAppState.current修改应用状态。
 * 使用staticCompositionLocalOf以提高性能，因为更新函数引用不会变化。
 */
val LocalUpdateAppState = staticCompositionLocalOf<((AppState) -> AppState) -> Unit> {
    error("No AppState updater provided!")
}
