package top.yukonga.miuix.kmp.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

/**
 * 换字动效的宿主。
 *
 * :miuix 内部画的文字（偏好项标题 / 副标题、下拉菜单当前值、弹窗标题 / 摘要）拿不到
 * :ui-shared 的 `TextMotionBox` —— 模块依赖方向不允许反向引用。所以由 App 在根部注册
 * 一个实现，这些「会变的字」统一交给它画：注册了就带动效，没注册就退回普通 [Text]，
 * 行为与改造前一致。
 */
typealias MiuixTextMotion = @Composable (
    text: String,
    modifier: Modifier,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
) -> Unit

/**
 * 换字动效实现，null 表示不接管（普通换字）。
 *
 * ⚠ 弹窗 / 下拉菜单渲染在 root host，读不到声明页面的 CompositionLocal，这个值必须
 * 随 `MiuixOverlayTheme` 一起快照过去（见 `MiuixOverlayHostState`）。
 *
 * static：注册一次就固定（顶层 val），不需要逐条文字建立重组依赖。
 */
val LocalMiuixTextMotion = staticCompositionLocalOf<MiuixTextMotion?> { null }
