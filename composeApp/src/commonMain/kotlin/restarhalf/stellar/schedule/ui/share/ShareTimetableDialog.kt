package restarhalf.stellar.schedule.ui.share

import restarhalf.stellar.schedule.ui.model.WeekHeaderUi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.ui.components.LocalLiquidGlassBackdrop
import restarhalf.stellar.schedule.ui.components.SheetCloseButton
import restarhalf.stellar.schedule.ui.components.SheetGlassButton
import restarhalf.stellar.schedule.ui.components.sheetContentBottomInset
import restarhalf.stellar.schedule.ui.icons.Save
import restarhalf.stellar.schedule.ui.mapper.DayRenderData
import top.yukonga.miuix.kmp.overlay.BlurBottomSheet
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.time.Clock

@Composable
fun ShareTimetableDialog(
    show: Boolean,
    nickname: String?,
    avatarUri: String?,
    weekLabel: String,
    weekHeaderUi: WeekHeaderUi,
    dayRenderData: ImmutableMap<Int, DayRenderData>,
    timetable: ImmutableList<TimetableSlot>,
    canSaveImage: Boolean,
    onSaveImage: suspend (fileName: String, bytes: ByteArray) -> Boolean,
    showMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    backgroundImageUri: String? = null,
    backgroundAlpha: Float = 1f,
    backgroundBlur: Float = 0f,
    backgroundBrightness: Float = 0f,
) {
    if (!show) return
    val scope = rememberCoroutineScope()
    val layer = rememberShareGraphicsLayer()
    val showSharer = !nickname.isNullOrBlank()
    val cardWidth = ShareCardWidth
    val cardHeight = shareCardHeight(showSharer)

    val saveAction: (@Composable () -> Unit)? =
        if (canSaveImage) {
            {
                SheetGlassButton(
                    icon = Save,
                    contentDescription = "保存",
                    onClick = {
                        scope.launch {
                            val result = runCatching {
                                val bitmap = withContext(Dispatchers.Default) {
                                    layer.toImageBitmap()
                                }
                                val bytes = encodeImageBitmapToJpeg(bitmap, quality = 92)
                                onSaveImage("Chrnova_Schedule_${Clock.System.now().toEpochMilliseconds()}.jpg", bytes)
                            }
                            result.fold(
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
                    modifier = Modifier.padding(end = 18.dp),
                )
            }
        } else {
            null
        }

    BlurBottomSheet(
        show = show,
        title = "分享课表",
        dimBackground = true,
        glassBackdrop = LocalLiquidGlassBackdrop.current,
        onDismissRequest = onDismiss,
        startAction = { SheetCloseButton(onDismissRequest = onDismiss) },
        endAction = saveAction,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .overScrollVertical()
                    .scrollEndHaptic(HapticFeedbackType.TextHandleMove)
                    .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(58.dp))
            val density = LocalDensity.current
            val screenHeight =
                with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
            val previewMaxHeight = (screenHeight - 260.dp).coerceIn(280.dp, 600.dp)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
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
                                backgroundBrightness = backgroundBrightness,
                                modifier = Modifier
                                    .requiredWidth(cardWidth)
                                    .requiredHeight(cardHeight),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(sheetContentBottomInset()))
        }
    }
}

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
