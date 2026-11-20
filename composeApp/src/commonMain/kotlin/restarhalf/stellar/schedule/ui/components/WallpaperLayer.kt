package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.haooz.softglass.backdrop.backdrops.LayerBackdrop
import com.haooz.softglass.backdrop.backdrops.layerBackdrop
import com.haooz.softglass.platform.isRenderEffectSupported
import restarhalf.stellar.schedule.domain.policy.WallpaperTransform

private const val WALLPAPER_BLUR_DP = 25f

@Composable
fun WallpaperLayer(
    image: ImageBitmap,
    alpha: Float,
    blur: Float,
    brightness: Float,
    modifier: Modifier = Modifier,
    offset: Offset = Offset.Zero,
    scale: Float = 0f,
    backdrop: LayerBackdrop? = null,
    baseColor: Color = Color.Transparent,
) {
    val density = LocalDensity.current
    val blurEffect =
        remember(blur, density) {
            val radiusPx = WALLPAPER_BLUR_DP * blur.coerceIn(0f, 1f) * density.density
            if (radiusPx > 0f && isRenderEffectSupported()) {
                BlurEffect(null, radiusPx, radiusPx, TileMode.Clamp)
            } else {
                null
            }
        }
    val colorFilter =
        remember(brightness) {
            if (brightness == 0f) {
                null
            } else {
                val b = (1f + brightness / 50f).coerceIn(0f, 2f)
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            b, 0f, 0f, 0f, 0f,
                            0f, b, 0f, 0f, 0f,
                            0f, 0f, b, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
            }
        }

    BoxWithConstraints(
        modifier.fillMaxSize()
            .then(
                if (backdrop != null) {
                    Modifier.layerBackdrop(
                        backdrop = backdrop,
                        recordKey = listOf(image, alpha, blur, brightness, offset, scale, baseColor),
                    )
                } else {
                    Modifier
                },
            )
            .drawBehind { drawRect(baseColor) },
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val minScale =
            remember(image, screenW, screenH) {
                WallpaperTransform.minScale(image.width, image.height, screenW, screenH)
            }
        val appliedScale = maxOf(scale, minScale)
        val (maxOffsetX, maxOffsetY) =
            remember(image, screenW, screenH, appliedScale) {
                WallpaperTransform.maxOffset(
                    bitmapW = image.width,
                    bitmapH = image.height,
                    screenW = screenW,
                    screenH = screenH,
                    scale = appliedScale,
                )
            }
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = alpha.coerceIn(0f, 1f)
                        scaleX = appliedScale
                        scaleY = appliedScale
                        translationX = offset.x.coerceIn(-maxOffsetX, maxOffsetX)
                        translationY = offset.y.coerceIn(-maxOffsetY, maxOffsetY)
                        clip = true
                        renderEffect = blurEffect
                    },
        )
    }
}
