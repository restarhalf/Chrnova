package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 应用Chrome状态
 * 
 * 管理应用的导航栏和顶部栏显示状态。
 * 
 * @param currentScreen 当前屏幕
 * @param isMainRoute 是否为主路由（决定是否显示底部导航栏）
 * @param barMode 导航栏模式（0=固定，1=悬浮，2=液态玻璃）
 */
@Stable
data class AppChromeState(
    val currentScreen: Screen,
    val isMainRoute: Boolean,
    val barMode: Int,
) {
    /** 是否显示导航栏 */
    val showNavigationChrome: Boolean
        get() = isMainRoute
}

/**
 * 应用Chrome状态的CompositionLocal
 */
val LocalAppChromeState = staticCompositionLocalOf<AppChromeState> {
    error("No AppChromeState provided!")
}

/**
 * 导航器的CompositionLocal
 */
val LocalNavigator = staticCompositionLocalOf<AppNavigator> {
    error("No AppNavigator provided!")
}
