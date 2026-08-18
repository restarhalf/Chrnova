package restarhalf.stellar.schedule.ui.blur

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalBlurEnabled = staticCompositionLocalOf { true }

@Composable
fun Modifier.defaultBlurEffect(
    backdrop: LayerBackdrop,
): Modifier = this.textureBlur(
    backdrop = backdrop,
    shape = RectangleShape,
    blurRadius = 25f,
    colors = BlurColors(
        blendColors = listOf(
            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.8f)),
        ),
    ),
)

@Composable
fun rememberBlurEnabled(): State<Boolean> =
    rememberUpdatedState(LocalBlurEnabled.current && isRuntimeShaderSupported())

@Composable
fun rememberBlurBackdrop(enabled: Boolean = LocalBlurEnabled.current): LayerBackdrop? {
    if (!enabled || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurEnabled: Boolean = rememberBlurEnabled().value,
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val blurActive = blurEnabled && backdrop != null
    Box(
        modifier = Modifier,
    ) {
        if (blurActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = scrollBehavior?.state
                            ?.let { (-it.contentOffset / 48.dp.toPx()).coerceIn(0f, 1f) }
                            ?: 1f
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        blurRadius = 10f,
                        colors = barBlurColors(progressive = true),
                    ),
            )
        }
        content()
    }
}

@Composable
private fun barBlurColors(progressive: Boolean = false): BlurColors = BlurDefaults.blurColors(
    blendColors = listOf(
        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(if (progressive) 0.3f else 0.8f)),
    ),
)
