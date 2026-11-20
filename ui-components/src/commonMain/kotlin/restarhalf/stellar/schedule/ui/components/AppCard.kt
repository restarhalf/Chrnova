package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.haooz.softglass.shape.ContinuousRoundedRectangle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalComponentsAlpha = compositionLocalOf { 1f }

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    colors: Color = MiuixTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    val alpha = LocalComponentsAlpha.current
    val wallpaperBackdrop = LocalWallpaperBackdrop.current
    val cardColor = colors.copy(alpha = alpha)
    if (wallpaperBackdrop != null && alpha < 0.999f) {
        val shape: Shape = remember { ContinuousRoundedRectangle(CardDefaults.CornerRadius) }
        CompositionLocalProvider(
            LocalContentColor provides MiuixTheme.colorScheme.onSurfaceContainer,
        ) {
            Box(
                modifier =
                    modifier
                        .semantics(mergeDescendants = false) { isTraversalGroup = true }
                        .wallpaperGlass(
                            backdrop = wallpaperBackdrop,
                            color = cardColor,
                            shape = shape,
                            cornerRadius = CardDefaults.CornerRadius,
                        ),
                propagateMinConstraints = true,
            ) {
                Column(content = content)
            }
        }
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.defaultColors(color = cardColor),
            content = content,
        )
    }
}
