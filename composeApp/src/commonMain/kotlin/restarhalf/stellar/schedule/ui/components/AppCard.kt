package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 组件透明度的CompositionLocal
 * 
 * 用于控制背景图片上组件的可见度。
 */
val LocalComponentsAlpha = compositionLocalOf { 1f }

/**
 * 应用卡片组件
 * 
 * 支持透明度控制的卡片组件，用于在背景图片上显示内容。
 * 
 * @param modifier Modifier修饰符
 * @param colors 卡片颜色
 * @param content 卡片内容
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    colors: Color = MiuixTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    val alpha = LocalComponentsAlpha.current
    Card(
        modifier = modifier,
        colors =
            CardDefaults.defaultColors(
                color = colors.copy(alpha = alpha)
            ),
        content = content
    )
}
