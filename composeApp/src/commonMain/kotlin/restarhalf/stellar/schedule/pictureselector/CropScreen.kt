package restarhalf.stellar.schedule.pictureselector

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Check
import restarhalf.stellar.schedule.ui.icons.Revert
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MaxZoom = 8f
private const val GestureGridHoldMs = 420L

private val StageInk = Color(0xFF0A0A0C)
private val DimScrim = Color.Black.copy(alpha = 0.55f)

/**
 * 图片裁剪页。
 *
 * 顶部沿用主题顶栏与原图标配色；下方为暗色裁剪舞台（窗外压暗）。
 * 手势期间淡入三分线，双指缩放、拖动平移、双击复位。
 */
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
    val colors = MiuixTheme.colorScheme
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
    val canConfirm = imageState is CropImageState.Ready && viewport != null && !isSaving
    val currentOnCropped by rememberUpdatedState(onCropped)

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "裁剪图片",
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回",
                            tint = colors.onBackground,
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = canConfirm,
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
                                        currentOnCropped(croppedUri)
                                    }
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Check,
                                contentDescription = "确定",
                                tint = colors.onBackground,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = imageState) {
                CropImageState.Loading -> CropStatusPane(title = "正在加载图片", showProgress = true)
                CropImageState.Error -> CropStatusPane(title = "无法加载这张图片", showProgress = false)
                is CropImageState.Ready -> {
                    CropStage(
                        preview = state.preview,
                        outputWidthPx = outputWidthPx,
                        outputHeightPx = outputHeightPx,
                        onViewportChanged = { viewport = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CropStatusPane(
    title: String,
    showProgress: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = title,
            style = MiuixTheme.textStyles.body2,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun CropBottomDock(
    zoom: Float,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val zoomLabel = remember(zoom) {
        val percent = (zoom * 100f).roundToInt()
        if (percent % 100 == 0) "${percent / 100}×" else "${percent}%"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(
            onClick = onReset,
            backgroundColor = colors.surfaceContainerHigh,
        ) {
            Icon(
                imageVector = Revert,
                contentDescription = "重置",
                tint = colors.onBackground,
            )
        }

        Text(
            text = zoomLabel,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.88f),
        )

        Text(
            text = "双指缩放 · 双击复位",
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.footnote2,
            color = Color.White.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun CropStage(
    preview: CropPreview,
    outputWidthPx: Int,
    outputHeightPx: Int,
    onViewportChanged: (CropViewportState) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val sourceSize = preview.sourceSize
        val previewWidthPx = preview.bitmap.width.coerceAtLeast(1)
        val previewHeightPx = preview.bitmap.height.coerceAtLeast(1)
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()

        val cropAspect = outputWidthPx.toFloat() / outputHeightPx.coerceAtLeast(1).toFloat()
        // 为顶部/底部悬浮控件留出空间，裁剪窗略偏上
        val maxFrameWidthPx = with(density) { (maxWidth - 36.dp).toPx().coerceAtLeast(1f) }
        val maxFrameHeightPx = with(density) { (maxHeight - 240.dp).toPx().coerceAtLeast(1f) }

        val frameSize = remember(maxFrameWidthPx, maxFrameHeightPx, cropAspect) {
            fitFrame(maxFrameWidthPx, maxFrameHeightPx, cropAspect)
        }
        val stageWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val stageHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
        val cropLeft = (stageWidthPx - frameSize.widthPx) / 2f
        val cropTop = (stageHeightPx - frameSize.heightPx) / 2f - with(density) { 12.dp.toPx() }

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
            previewHeightPx,
        ) { mutableStateOf(1f) }

        fun centeredOffset(zoomValue: Float): Offset {
            val scale = baseScale * zoomValue
            val displayWidth = previewWidthPx * scale
            val displayHeight = previewHeightPx * scale
            return Offset(
                x = cropLeft + (frameSize.widthPx - displayWidth) / 2f,
                y = cropTop + (frameSize.heightPx - displayHeight) / 2f,
            )
        }

        var offsetX by remember(
            previewWidthPx,
            previewHeightPx,
            frameSize.widthPx,
            frameSize.heightPx,
            cropLeft,
            cropTop,
        ) {
            mutableStateOf(centeredOffset(1f).x)
        }
        var offsetY by remember(
            previewWidthPx,
            previewHeightPx,
            frameSize.widthPx,
            frameSize.heightPx,
            cropLeft,
            cropTop,
        ) {
            mutableStateOf(centeredOffset(1f).y)
        }
        var isInteracting by remember { mutableStateOf(false) }
        var hideGridJob by remember { mutableStateOf<Job?>(null) }

        val displayWidthPx = previewWidthPx * baseScale * zoom
        val displayHeightPx = previewHeightPx * baseScale * zoom

        fun markInteracting() {
            isInteracting = true
            hideGridJob?.cancel()
            hideGridJob = scope.launch {
                delay(GestureGridHoldMs)
                isInteracting = false
            }
        }

        fun resetTransform() {
            markInteracting()
            zoom = 1f
            val centered = centeredOffset(1f)
            offsetX = centered.x
            offsetY = centered.y
            onViewportChanged(
                CropViewportState(
                    sourceSize = sourceSize,
                    previewWidthPx = previewWidthPx,
                    previewHeightPx = previewHeightPx,
                    cropLeftPx = cropLeft,
                    cropTopPx = cropTop,
                    cropWidthPx = frameSize.widthPx,
                    cropHeightPx = frameSize.heightPx,
                    baseScale = baseScale,
                    zoom = 1f,
                    offsetX = centered.x,
                    offsetY = centered.y,
                ),
            )
        }

        fun publishViewport() {
            onViewportChanged(
                CropViewportState(
                    sourceSize = sourceSize,
                    previewWidthPx = previewWidthPx,
                    previewHeightPx = previewHeightPx,
                    cropLeftPx = cropLeft,
                    cropTopPx = cropTop,
                    cropWidthPx = frameSize.widthPx,
                    cropHeightPx = frameSize.heightPx,
                    baseScale = baseScale,
                    zoom = zoom,
                    offsetX = offsetX,
                    offsetY = offsetY,
                ),
            )
        }

        LaunchedEffect(
            sourceSize.width,
            sourceSize.height,
            previewWidthPx,
            previewHeightPx,
            frameSize.widthPx,
            frameSize.heightPx,
            cropLeft,
            cropTop,
            zoom,
        ) {
            val clamped = clampOffsetToCrop(
                offset = Offset(offsetX, offsetY),
                cropLeft = cropLeft,
                cropTop = cropTop,
                cropWidth = frameSize.widthPx,
                cropHeight = frameSize.heightPx,
                displayWidth = displayWidthPx,
                displayHeight = displayHeightPx,
            )
            offsetX = clamped.x
            offsetY = clamped.y
            publishViewport()
        }

        val gridAlpha by animateFloatAsState(
            targetValue = if (isInteracting) 1f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "cropGridAlpha",
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        sourceSize,
                        previewWidthPx,
                        previewHeightPx,
                        frameSize.widthPx,
                        frameSize.heightPx,
                        cropLeft,
                        cropTop,
                        baseScale,
                    ) {
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            markInteracting()
                            val oldZoom = zoom
                            val oldScale = baseScale * oldZoom
                            val oldOffset = Offset(offsetX, offsetY)

                            val focusSourceX = ((centroid.x - oldOffset.x) / oldScale)
                                .coerceIn(0f, previewWidthPx.toFloat())
                            val focusSourceY = ((centroid.y - oldOffset.y) / oldScale)
                                .coerceIn(0f, previewHeightPx.toFloat())

                            val newZoom = (oldZoom * gestureZoom).coerceIn(1f, MaxZoom)
                            val newScale = baseScale * newZoom
                            val newDisplayWidth = previewWidthPx * newScale
                            val newDisplayHeight = previewHeightPx * newScale

                            val unclampedOffset = Offset(
                                x = centroid.x - focusSourceX * newScale + pan.x,
                                y = centroid.y - focusSourceY * newScale + pan.y,
                            )
                            val clamped = clampOffsetToCrop(
                                offset = unclampedOffset,
                                cropLeft = cropLeft,
                                cropTop = cropTop,
                                cropWidth = frameSize.widthPx,
                                cropHeight = frameSize.heightPx,
                                displayWidth = newDisplayWidth,
                                displayHeight = newDisplayHeight,
                            )

                            zoom = newZoom
                            offsetX = clamped.x
                            offsetY = clamped.y
                            publishViewport()
                        }
                    }
                    .pointerInput(
                        sourceSize,
                        previewWidthPx,
                        previewHeightPx,
                        frameSize.widthPx,
                        frameSize.heightPx,
                        cropLeft,
                        cropTop,
                    ) {
                        detectTapGestures(onDoubleTap = { resetTransform() })
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawImage(
                        image = preview.bitmap,
                        dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                        dstSize = IntSize(
                            displayWidthPx.roundToInt().coerceAtLeast(1),
                            displayHeightPx.roundToInt().coerceAtLeast(1),
                        ),
                    )
                }

                CropOverlay(
                    cropLeft = cropLeft,
                    cropTop = cropTop,
                    cropWidth = frameSize.widthPx,
                    cropHeight = frameSize.heightPx,
                    gridAlpha = gridAlpha,
                )
            }

            CropBottomDock(
                zoom = zoom,
                onReset = { resetTransform() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CropOverlay(
    cropLeft: Float,
    cropTop: Float,
    cropWidth: Float,
    cropHeight: Float,
    gridAlpha: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cropRect = Rect(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight)

        val dimPath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, size.width, size.height))
            addRect(cropRect)
        }
        drawPath(path = dimPath, color = DimScrim)

        drawRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(cropLeft, cropTop),
            size = Size(cropWidth, cropHeight),
            style = Stroke(width = 1.5.dp.toPx()),
        )

        if (gridAlpha > 0.01f) {
            val gridColor = Color.White.copy(alpha = 0.22f * gridAlpha)
            val gridStroke = 1.dp.toPx()
            for (i in 1..2) {
                val x = cropLeft + cropWidth * i / 3f
                val y = cropTop + cropHeight * i / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(x, cropTop),
                    end = Offset(x, cropTop + cropHeight),
                    strokeWidth = gridStroke,
                )
                drawLine(
                    color = gridColor,
                    start = Offset(cropLeft, y),
                    end = Offset(cropLeft + cropWidth, y),
                    strokeWidth = gridStroke,
                )
            }
        }

        val cornerLen = min(cropWidth, cropHeight) * 0.12f
        val cornerStroke = 3.5.dp.toPx()
        val cornerColor = Color.White.copy(alpha = 0.92f + 0.08f * gridAlpha)

        // 四角 L 形手柄
        drawLine(cornerColor, Offset(cropLeft, cropTop), Offset(cropLeft + cornerLen, cropTop), cornerStroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(cropLeft, cropTop), Offset(cropLeft, cropTop + cornerLen), cornerStroke, StrokeCap.Round)
        drawLine(
            cornerColor,
            Offset(cropLeft + cropWidth, cropTop),
            Offset(cropLeft + cropWidth - cornerLen, cropTop),
            cornerStroke,
            StrokeCap.Round,
        )
        drawLine(
            cornerColor,
            Offset(cropLeft + cropWidth, cropTop),
            Offset(cropLeft + cropWidth, cropTop + cornerLen),
            cornerStroke,
            StrokeCap.Round,
        )
        drawLine(
            cornerColor,
            Offset(cropLeft, cropTop + cropHeight),
            Offset(cropLeft + cornerLen, cropTop + cropHeight),
            cornerStroke,
            StrokeCap.Round,
        )
        drawLine(
            cornerColor,
            Offset(cropLeft, cropTop + cropHeight),
            Offset(cropLeft, cropTop + cropHeight - cornerLen),
            cornerStroke,
            StrokeCap.Round,
        )
        drawLine(
            cornerColor,
            Offset(cropLeft + cropWidth, cropTop + cropHeight),
            Offset(cropLeft + cropWidth - cornerLen, cropTop + cropHeight),
            cornerStroke,
            StrokeCap.Round,
        )
        drawLine(
            cornerColor,
            Offset(cropLeft + cropWidth, cropTop + cropHeight),
            Offset(cropLeft + cropWidth, cropTop + cropHeight - cornerLen),
            cornerStroke,
            StrokeCap.Round,
        )
    }
}

private fun aspectLabel(widthPx: Int, heightPx: Int): String {
    val w = widthPx.coerceAtLeast(1)
    val h = heightPx.coerceAtLeast(1)
    val g = gcd(w, h)
    val rw = w / g
    val rh = h / g
    return if (rw <= 32 && rh <= 32) "$rw:$rh" else "$w×$h"
}

private fun gcd(a: Int, b: Int): Int {
    var x = a
    var y = b
    while (y != 0) {
        val t = x % y
        x = y
        y = t
    }
    return x
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

/** 保证图片始终覆盖裁剪窗（而不是整个舞台）。 */
private fun clampOffsetToCrop(
    offset: Offset,
    cropLeft: Float,
    cropTop: Float,
    cropWidth: Float,
    cropHeight: Float,
    displayWidth: Float,
    displayHeight: Float,
): Offset {
    val minX = cropLeft + cropWidth - displayWidth
    val maxX = cropLeft
    val minY = cropTop + cropHeight - displayHeight
    val maxY = cropTop
    return Offset(
        x = offset.x.coerceIn(min(minX, maxX), max(minX, maxX)),
        y = offset.y.coerceIn(min(minY, maxY), max(minY, maxY)),
    )
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
    val cropLeftPx: Float,
    val cropTopPx: Float,
    val cropWidthPx: Float,
    val cropHeightPx: Float,
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
        val previewLeft = ((cropLeftPx - offsetX) / actualScale)
            .coerceIn(0f, previewWidthPx.toFloat())
        val previewTop = ((cropTopPx - offsetY) / actualScale)
            .coerceIn(0f, previewHeightPx.toFloat())
        val previewWidth = (cropWidthPx / actualScale)
            .coerceIn(1f, previewWidthPx.toFloat() - previewLeft)
        val previewHeight = (cropHeightPx / actualScale)
            .coerceIn(1f, previewHeightPx.toFloat() - previewTop)
        val scaleX = sourceSize.width.toFloat() / previewWidthPx.coerceAtLeast(1).toFloat()
        val scaleY = sourceSize.height.toFloat() / previewHeightPx.coerceAtLeast(1).toFloat()
        val sourceLeft = previewLeft * scaleX
        val sourceTop = previewTop * scaleY
        val sourceWidth = previewWidth * scaleX
        val sourceHeight = previewHeight * scaleY
        val cropLeftPxInt = sourceLeft.toInt().coerceIn(0, sourceSize.width - 1)
        val cropTopPxInt = sourceTop.toInt().coerceIn(0, sourceSize.height - 1)
        val cropWidthPxInt =
            ceil(sourceWidth).toInt().coerceIn(1, sourceSize.width - cropLeftPxInt)
        val cropHeightPxInt =
            ceil(sourceHeight).toInt().coerceIn(1, sourceSize.height - cropTopPxInt)

        return CropRequest(
            sourceUri = imageUri,
            sourceWidthPx = sourceSize.width,
            sourceHeightPx = sourceSize.height,
            outputWidthPx = outputWidthPx.coerceAtLeast(1),
            outputHeightPx = outputHeightPx.coerceAtLeast(1),
            sourceCropLeftPx = cropLeftPxInt,
            sourceCropTopPx = cropTopPxInt,
            sourceCropWidthPx = cropWidthPxInt,
            sourceCropHeightPx = cropHeightPxInt,
        )
    }
}
