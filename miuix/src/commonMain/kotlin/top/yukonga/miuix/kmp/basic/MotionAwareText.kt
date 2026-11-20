package top.yukonga.miuix.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnit.Companion.Unspecified
import top.yukonga.miuix.kmp.material.LocalMiuixTextMotion

/**
 * 展示用文字：宿主注册了换字动效就走它，否则原样 [Text]。
 *
 * 只用于**不会由用户逐字输入**的文案（偏好项标题 / 副标题、弹窗标题 / 摘要）；组件内所有
 * 这类文字都必须经过这里，避免同一段文案有的换字带模糊、有的硬切。
 */
@Composable
internal fun MotionAwareText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    val host = LocalMiuixTextMotion.current
    if (host != null) {
        host(text, modifier, color, fontSize, fontWeight, textAlign)
    } else {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
        )
    }
}
