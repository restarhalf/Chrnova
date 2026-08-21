package restarhalf.stellar.schedule.ui.image

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * 可缩放/平移的网络图片组件（QQ 看图器核心）。
 *
 * 复用 pictureselector 模块 [CropScreen] 的缩放/平移手势算法
 * （[detectTransformGestures] + 边界 clamp），区别在于此处没有裁剪框，
 * 而是以"容器居中 fit"为基准，缩放在 1x–[MaxZoom] 之间，平移时保证图片
 * 边缘不回缩进可视区（小于容器时强制居中）。单指点按切状态栏，双击在
 * 1x / 2.5x 间切换。
 */
@Composable
fun ZoomableAsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().clipToBounds(),
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()
        var intrinsic by remember(url) { mutableStateOf<IntSize?>(null) }
        var zoom by remember(url) { mutableStateOf(1f) }
        var offset by remember(url) { mutableStateOf(Offset.Zero) }

        fun clamp(newZoom: Float, rawOffset: Offset): Pair<Float, Offset> {
            val z = newZoom.coerceIn(1f, MaxZoom)
            val size = intrinsic
            if (size == null || size.width <= 0 || size.height <= 0) {
                return z to Offset.Zero
            }
            // 以"居中 fit"为基准的显示尺寸
            val fitScale = min(containerW / size.width, containerH / size.height)
                .coerceAtLeast(1e-3f)
            val dispW = size.width * fitScale * z
            val dispH = size.height * fitScale * z
            val maxX = max(0f, (dispW - containerW) / 2f)
            val maxY = max(0f, (dispH - containerH) / 2f)
            val ox = if (z <= 1f) 0f else rawOffset.x.coerceIn(-maxX, maxX)
            val oy = if (z <= 1f) 0f else rawOffset.y.coerceIn(-maxY, maxY)
            return z to Offset(ox, oy)
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin.Center
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val (z, o) = clamp(zoom * gestureZoom, offset + pan)
                            zoom = z
                            offset = o
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (zoom > 1f) {
                                    zoom = 1f
                                    offset = Offset.Zero
                                } else {
                                    val (z, o) = clamp(2.5f, Offset.Zero)
                                    zoom = z
                                    offset = o
                                }
                            },
                            onTap = { onTap() },
                        )
                    },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model =
                    ImageRequest.Builder(LocalPlatformContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { state ->
                    val painter = state.painter
                    if (painter.intrinsicSize != Size.Unspecified) {
                        intrinsic =
                            IntSize(
                                painter.intrinsicSize.width.roundToInt(),
                                painter.intrinsicSize.height.roundToInt(),
                            )
                    }
                },
            )
        }
    }
}

private const val MaxZoom = 5f
