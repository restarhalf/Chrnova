package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalComponentsAlpha = compositionLocalOf { 1f }

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
