package top.yukonga.miuix.kmp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.concurrent.Volatile

/**
 * 强制深色主题的覆盖开关。null = 不覆盖（跟随外层主题）。
 * BlurBottomSheet 内部提供 null，阻止外层 App 的强制深色渗进弹窗；MiuixTheme 的 forceDark 也读它。
 */
val LocalForcedDarkTheme = staticCompositionLocalOf<Boolean?> { null }

/**
 * 宿主下发的「是否深色」。默认 `null` 表示未提供（跟随系统），不能默认 `false`。
 *
 * 弹窗会把它覆盖成 `null`，所以读用户偏好要走 [AppSettingDarkBridge]。
 */
val LocalAppSettingDark = staticCompositionLocalOf<Boolean?> { null }

/**
 * 宿主注入的深浅色数据源。本模块不依赖 `:core`，读不到存储。
 *
 * [rememberAppSettingDark] 的取值顺序：注入的 [read] → [LocalAppSettingDark] → 系统深浅色。
 */
object AppSettingDarkBridge {

    /** 返回 `"dark"` / `"light"` / 其它（视为跟随系统）。未注入为 null。 */
    @Volatile
    var read: (() -> String)? = null

    /** 订阅变化，返回取消订阅。未注入时不监听。 */
    @Volatile
    var subscribe: ((onChange: () -> Unit) -> (() -> Unit))? = null
}

/**
 * App 偏好里的「是否深色」（跟随系统 / 浅 / 深）。数据经 [AppSettingDarkBridge] 注入。
 */
@Composable
fun rememberAppSettingDark(): Boolean {
    val read = AppSettingDarkBridge.read ?: return LocalAppSettingDark.current ?: isSystemInDarkTheme()
    val mode = remember(read) { mutableStateOf(read()) }
    DisposableEffect(read) {
        val unsubscribe = AppSettingDarkBridge.subscribe?.invoke { mode.value = read() }
        onDispose { unsubscribe?.invoke() }
    }
    return when (mode.value) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
}

/**
 * 宿主是否**真的**在用系统壁纸取色（Monet）—— 偏好已开启且当前平台支持
 * （Android 要求 SDK >= 31，其它平台恒为 false）。
 *
 * 默认 `false`：不取色即走内置亮暗主题。
 */
val LocalMonetColorEnabled = staticCompositionLocalOf { false }
