package restarhalf.stellar.schedule.ui.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.core.image.encodeImageBitmapToJpeg
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.ui.icons.Close
import restarhalf.stellar.schedule.ui.icons.Save
import restarhalf.stellar.schedule.ui.mapper.DayRenderData
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import kotlin.time.Clock

/**
 * 课表分享预览与导出。
 *
 * 卡片按 [ShareCardWidth] 固定尺寸离屏合成（GraphicsLayer，等价 ImageComposeScene），
 * 预览只做等比缩放，导出 PNG 始终是完整尺寸。
 */
@Composable
fun ShareTimetableDialog(
    show: Boolean,
    nickname: String?,
    avatarUri: String?,
    weekLabel: String,
    weekHeaderUi: ScheduleViewModel.WeekHeaderUi,
    dayRenderData: ImmutableMap<Int, DayRenderData>,
    timetable: ImmutableList<TimetableSlot>,
    canSaveImage: Boolean,
    onSaveImage: suspend (fileName: String, bytes: ByteArray) -> Boolean,
    showMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    backgroundImageUri: String? = null,
    backgroundAlpha: Float = 1f,
    backgroundBlur: Float = 0f,
) {
    if (!show) return
    val scope = rememberCoroutineScope()
    val layer = rememberShareGraphicsLayer()
    val showSharer = !nickname.isNullOrBlank()
    val cardWidth = ShareCardWidth
    val cardHeight = shareCardHeight(showSharer)

    OverlayBottomSheet(
        show = show,
        modifier = Modifier,
        title = "分享课表",
        startAction = {
            IconButton(onClick = onDismiss){
                Icon(imageVector = Close, contentDescription = "关闭")
            }
        },
        endAction = {
            IconButton(
                onClick = {
                scope.launch {
                    val result = runCatching {
                        val bitmap = withContext(Dispatchers.Default) {
                            layer.toImageBitmap()
                        }
                        val bytes = encodeImageBitmapToJpeg(bitmap, quality = 92)
                        onSaveImage("Chrnova_Schedule_${Clock.System.now().toEpochMilliseconds()}.jpg", bytes)
                    }
                    result.onFailure {
                        AppLogger.log("Share", "导出课表分享图失败", it)
                    }.fold(
                        onSuccess = { saved ->
                            showMessage(if (saved) "图片已保存" else "保存失败，请重试")
                            if (saved) onDismiss()
                        },
                        onFailure = {
                            showMessage("导出失败，请重试")
                        },
                    )
                }
            },
                enabled = canSaveImage
            ){
                Icon(imageVector = Save, contentDescription = "保存")
            }
        },
        backgroundColor = BottomSheetDefaults.backgroundColor(),
        enableWindowDim = true,
        cornerRadius = BottomSheetDefaults.cornerRadius,
        sheetMaxWidth = BottomSheetDefaults.maxWidth,
        onDismissRequest = onDismiss,
        onDismissFinished = null,
        outsideMargin = BottomSheetDefaults.outsideMargin,
        insideMargin = BottomSheetDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        renderInRootScaffold = true,
        content = {
            val density = LocalDensity.current
            val screenHeight =
                with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
            val previewMaxHeight = (screenHeight * 0.8f).coerceIn(280.dp, 640.dp)

            Column(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = previewMaxHeight),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    ScaleToFitBox(
                        contentWidth = cardWidth,
                        contentHeight = cardHeight,
                        maxWidth = maxWidth,
                        maxHeight = maxHeight,
                    ) {
                        ShareCaptureBox(
                            layer = layer,
                            modifier = Modifier
                                .requiredWidth(cardWidth)
                                .requiredHeight(cardHeight),
                        ) {
                            ShareTimetableCard(
                                nickname = nickname,
                                avatarUri = avatarUri,
                                weekLabel = weekLabel,
                                weekHeaderUi = weekHeaderUi,
                                dayRenderData = dayRenderData,
                                timetable = timetable,
                                backgroundImageUri = backgroundImageUri,
                                backgroundAlpha = backgroundAlpha,
                                backgroundBlur = backgroundBlur,
                                modifier = Modifier
                                    .requiredWidth(cardWidth)
                                    .requiredHeight(cardHeight),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
        },
    )
}

/**
 * 子内容按 [contentWidth]×[contentHeight] 绘制，在 [maxWidth]×[maxHeight] 内等比缩小到一屏可放下。
 * 预览变小，但 [GraphicsLayer] 记录的仍是完整卡片像素。
 */
@Composable
private fun ScaleToFitBox(
    contentWidth: Dp,
    contentHeight: Dp,
    maxWidth: Dp,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.layout { measurable, _ ->
            val fullW = contentWidth.roundToPx()
            val fullH = contentHeight.roundToPx()
            val limitW = maxWidth.roundToPx()
            val limitH = maxHeight.roundToPx()
            val scale = if (fullW <= 0 || fullH <= 0) {
                1f
            } else {
                minOf(
                    limitW.toFloat() / fullW,
                    limitH.toFloat() / fullH,
                    1f,
                ).coerceAtLeast(0.15f)
            }
            val displayW = (fullW * scale).toInt().coerceAtLeast(1)
            val displayH = (fullH * scale).toInt().coerceAtLeast(1)
            val placeable = measurable.measure(Constraints.fixed(fullW, fullH))
            layout(displayW, displayH) {
                placeable.placeWithLayer(0, 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                }
            }
        }
    ) {
        content()
    }
}
