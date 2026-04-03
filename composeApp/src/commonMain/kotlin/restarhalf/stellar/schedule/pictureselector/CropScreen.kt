package restarhalf.stellar.schedule.pictureselector

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.icons.Check
import restarhalf.stellar.schedule.ui.icons.Close
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixShape
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MaxZoom = 8f

@Composable
fun CropScreen(
    imageUri: String,
    outputWidthPx: Int,
    outputHeightPx: Int,
    port: PictureSelectorPort,
    onCancel: () -> Unit,
    onCropped: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isSaving by rememberSaveable(imageUri) { mutableStateOf(false) }
    val imageState by produceState<CropImageState>(
        initialValue = CropImageState.Loading,
        key1 = imageUri,
    ) {
        value =
            withContext(AppIoDispatcher) {
                port.loadCropPreview(
                    uriString = imageUri,
                    maxSidePx = max(outputWidthPx, outputHeightPx).coerceAtLeast(1),
                )?.let { CropImageState.Ready(it) }
                    ?: CropImageState.Error
            }
    }
    var viewport by remember(imageUri) { mutableStateOf<CropViewportState?>(null) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "裁剪图片",
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Close,
                            contentDescription = "关闭",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (imageState !is CropImageState.Ready || isSaving) return@IconButton
                            val currentViewport = viewport ?: return@IconButton
                            scope.launch {
                                isSaving = true
                                try {
                                    val croppedUri =
                                        withContext(AppIoDispatcher) {
                                            port.cropAndWriteJpegToCache(
                                                currentViewport.buildCropRequest(
                                                    imageUri = imageUri,
                                                    outputWidthPx = outputWidthPx,
                                                    outputHeightPx = outputHeightPx,
                                                ),
                                            )
                                        }
                                    if (croppedUri != null) {
                                        onCropped(croppedUri)
                                    }
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Check,
                            contentDescription = "确定",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(innerPadding)
                    .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = imageState) {
                CropImageState.Loading -> Text(text = "加载中...", color = Color.White)
                CropImageState.Error -> Text(text = "无法加载图片", color = Color.White)
                is CropImageState.Ready -> {
                    CropViewport(
                        preview = state.preview,
                        outputWidthPx = outputWidthPx,
                        outputHeightPx = outputHeightPx,
                        onViewportChanged = { viewport = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun CropViewport(
    preview: CropPreview,
    outputWidthPx: Int,
    outputHeightPx: Int,
    onViewportChanged: (CropViewportState) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val sourceSize = preview.sourceSize
        val previewWidthPx = preview.bitmap.width.coerceAtLeast(1)
        val previewHeightPx = preview.bitmap.height.coerceAtLeast(1)
        val density = LocalDensity.current
        val cropAspect = outputWidthPx.toFloat() / outputHeightPx.coerceAtLeast(1).toFloat()
        val maxFrameWidthPx =
            with(density) { (maxWidth - 24.dp * 2).toPx().coerceAtLeast(1f) }
        val maxFrameHeightPx =
            with(density) { (maxHeight - 24.dp * 2).toPx().coerceAtLeast(1f) }

        val frameSize = remember(maxFrameWidthPx, maxFrameHeightPx, cropAspect) {
            fitFrame(maxFrameWidthPx, maxFrameHeightPx, cropAspect)
        }
        val baseScale =
            remember(frameSize.widthPx, frameSize.heightPx, previewWidthPx, previewHeightPx) {
                max(
                    frameSize.widthPx / previewWidthPx.toFloat(),
                    frameSize.heightPx / previewHeightPx.toFloat(),
                )
            }

        var zoom by rememberSaveable(
            sourceSize.width,
            sourceSize.height,
            previewWidthPx,
            previewHeightPx
        ) { mutableStateOf(1f) }
        var offsetX by remember(
            previewWidthPx,
            previewHeightPx,
            frameSize.widthPx,
            frameSize.heightPx
        ) {
            mutableStateOf((frameSize.widthPx - previewWidthPx * baseScale) / 2f)
        }
        var offsetY by remember(
            previewWidthPx,
            previewHeightPx,
            frameSize.widthPx,
            frameSize.heightPx
        ) {
            mutableStateOf((frameSize.heightPx - previewHeightPx * baseScale) / 2f)
        }

        val displayWidthPx = previewWidthPx * baseScale * zoom
        val displayHeightPx = previewHeightPx * baseScale * zoom

        LaunchedEffect(
            sourceSize.width,
            sourceSize.height,
            previewWidthPx,
            previewHeightPx,
            frameSize.widthPx,
            frameSize.heightPx,
            zoom
        ) {
            val clamped =
                clampOffset(
                    offset = Offset(offsetX, offsetY),
                    frameWidth = frameSize.widthPx,
                    frameHeight = frameSize.heightPx,
                    displayWidth = displayWidthPx,
                    displayHeight = displayHeightPx,
                )
            offsetX = clamped.x
            offsetY = clamped.y
            onViewportChanged(
                CropViewportState(
                    sourceSize = sourceSize,
                    previewWidthPx = previewWidthPx,
                    previewHeightPx = previewHeightPx,
                    frameWidthPx = frameSize.widthPx,
                    frameHeightPx = frameSize.heightPx,
                    baseScale = baseScale,
                    zoom = zoom,
                    offsetX = offsetX,
                    offsetY = offsetY,
                ),
            )
        }

        val frameWidthDp = with(density) { frameSize.widthPx.toDp() }
        val frameHeightDp = with(density) { frameSize.heightPx.toDp() }
        Box(
            modifier =
                Modifier
                    .size(frameWidthDp, frameHeightDp)
                    .clip(miuixShape(2.dp))
                    .background(Color.Black)
                    .pointerInput(
                        sourceSize,
                        previewWidthPx,
                        previewHeightPx,
                        frameSize.widthPx,
                        frameSize.heightPx,
                        baseScale
                    ) {
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            val oldZoom = zoom
                            val oldScale = baseScale * oldZoom
                            val oldOffset = Offset(offsetX, offsetY)

                            val focusSourceX =
                                ((centroid.x - oldOffset.x) / oldScale)
                                    .coerceIn(0f, previewWidthPx.toFloat())
                            val focusSourceY =
                                ((centroid.y - oldOffset.y) / oldScale)
                                    .coerceIn(0f, previewHeightPx.toFloat())

                            val newZoom = (oldZoom * gestureZoom).coerceIn(1f, MaxZoom)
                            val newScale = baseScale * newZoom
                            val newDisplayWidth = previewWidthPx * newScale
                            val newDisplayHeight = previewHeightPx * newScale

                            val unclampedOffset =
                                Offset(
                                    x = centroid.x - focusSourceX * newScale + pan.x,
                                    y = centroid.y - focusSourceY * newScale + pan.y,
                                )
                            val clamped =
                                clampOffset(
                                    offset = unclampedOffset,
                                    frameWidth = frameSize.widthPx,
                                    frameHeight = frameSize.heightPx,
                                    displayWidth = newDisplayWidth,
                                    displayHeight = newDisplayHeight,
                                )

                            zoom = newZoom
                            offsetX = clamped.x
                            offsetY = clamped.y
                            onViewportChanged(
                                CropViewportState(
                                    sourceSize = sourceSize,
                                    previewWidthPx = previewWidthPx,
                                    previewHeightPx = previewHeightPx,
                                    frameWidthPx = frameSize.widthPx,
                                    frameHeightPx = frameSize.heightPx,
                                    baseScale = baseScale,
                                    zoom = zoom,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                ),
                            )
                        }
                    },
        ) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                drawImage(
                    image = preview.bitmap,
                    dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                    dstSize = IntSize(
                        displayWidthPx.roundToInt().coerceAtLeast(1),
                        displayHeightPx.roundToInt().coerceAtLeast(1),
                    ),
                )
            }
            CropFrameOverlay()
        }
    }
}

@Composable
private fun CropFrameOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            color = Color.White.copy(alpha = 0.92f),
            size = size,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
        )

        val corner = min(size.width, size.height) * 0.08f
        val strokeWidth = 4.dp.toPx()

        drawLine(Color.White, Offset.Zero, Offset(corner, 0f), strokeWidth, StrokeCap.Round)
        drawLine(Color.White, Offset.Zero, Offset(0f, corner), strokeWidth, StrokeCap.Round)
        drawLine(
            Color.White,
            Offset(size.width, 0f),
            Offset(size.width - corner, 0f),
            strokeWidth,
            StrokeCap.Round,
        )
        drawLine(
            Color.White,
            Offset(size.width, 0f),
            Offset(size.width, corner),
            strokeWidth,
            StrokeCap.Round,
        )
        drawLine(
            Color.White,
            Offset(0f, size.height),
            Offset(corner, size.height),
            strokeWidth,
            StrokeCap.Round,
        )
        drawLine(
            Color.White,
            Offset(0f, size.height),
            Offset(0f, size.height - corner),
            strokeWidth,
            StrokeCap.Round,
        )
        drawLine(
            Color.White,
            Offset(size.width, size.height),
            Offset(size.width - corner, size.height),
            strokeWidth,
            StrokeCap.Round,
        )
        drawLine(
            Color.White,
            Offset(size.width, size.height),
            Offset(size.width, size.height - corner),
            strokeWidth,
            StrokeCap.Round,
        )
    }
}

private fun fitFrame(
    maxWidthPx: Float,
    maxHeightPx: Float,
    aspectRatio: Float,
): FrameSize {
    return if (maxWidthPx / maxHeightPx > aspectRatio) {
        FrameSize(widthPx = maxHeightPx * aspectRatio, heightPx = maxHeightPx)
    } else {
        FrameSize(widthPx = maxWidthPx, heightPx = maxWidthPx / aspectRatio)
    }
}

private fun clampOffset(
    offset: Offset,
    frameWidth: Float,
    frameHeight: Float,
    displayWidth: Float,
    displayHeight: Float,
): Offset {
    val clampedX =
        if (displayWidth <= frameWidth) {
            (frameWidth - displayWidth) / 2f
        } else {
            offset.x.coerceIn(frameWidth - displayWidth, 0f)
        }
    val clampedY =
        if (displayHeight <= frameHeight) {
            (frameHeight - displayHeight) / 2f
        } else {
            offset.y.coerceIn(frameHeight - displayHeight, 0f)
        }
    return Offset(clampedX, clampedY)
}

private data class FrameSize(
    val widthPx: Float,
    val heightPx: Float,
)

private sealed interface CropImageState {
    data object Loading : CropImageState
    data object Error : CropImageState
    data class Ready(
        val preview: CropPreview,
    ) : CropImageState
}

data class CropViewportState(
    val sourceSize: ImageSize,
    val previewWidthPx: Int,
    val previewHeightPx: Int,
    val frameWidthPx: Float,
    val frameHeightPx: Float,
    val baseScale: Float,
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float,
) {
    fun buildCropRequest(
        imageUri: String,
        outputWidthPx: Int,
        outputHeightPx: Int,
    ): CropRequest {
        val actualScale = baseScale * zoom
        val previewLeft = (-offsetX / actualScale).coerceIn(0f, previewWidthPx.toFloat())
        val previewTop = (-offsetY / actualScale).coerceIn(0f, previewHeightPx.toFloat())
        val previewWidth =
            (frameWidthPx / actualScale)
                .coerceIn(1f, previewWidthPx.toFloat() - previewLeft)
        val previewHeight =
            (frameHeightPx / actualScale)
                .coerceIn(1f, previewHeightPx.toFloat() - previewTop)
        val scaleX = sourceSize.width.toFloat() / previewWidthPx.coerceAtLeast(1).toFloat()
        val scaleY = sourceSize.height.toFloat() / previewHeightPx.coerceAtLeast(1).toFloat()
        val sourceLeft = previewLeft * scaleX
        val sourceTop = previewTop * scaleY
        val sourceWidth = previewWidth * scaleX
        val sourceHeight = previewHeight * scaleY
        val cropLeftPx = sourceLeft.toInt().coerceIn(0, sourceSize.width - 1)
        val cropTopPx = sourceTop.toInt().coerceIn(0, sourceSize.height - 1)
        val cropWidthPx =
            ceil(sourceWidth).toInt().coerceIn(1, sourceSize.width - cropLeftPx)
        val cropHeightPx =
            ceil(sourceHeight).toInt().coerceIn(1, sourceSize.height - cropTopPx)

        return CropRequest(
            sourceUri = imageUri,
            sourceWidthPx = sourceSize.width,
            sourceHeightPx = sourceSize.height,
            outputWidthPx = outputWidthPx.coerceAtLeast(1),
            outputHeightPx = outputHeightPx.coerceAtLeast(1),
            sourceCropLeftPx = cropLeftPx,
            sourceCropTopPx = cropTopPx,
            sourceCropWidthPx = cropWidthPx,
            sourceCropHeightPx = cropHeightPx,
        )
    }
}
