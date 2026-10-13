package restarhalf.stellar.schedule.ui.components.screen.schedule

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.components.WeekPalette
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WeekPickerSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    totalWeeks: Int,
    viewingWeek: Int,
    detectedWeek: Int,
    onWeekSelected: (Int) -> Unit,
) {
    OverlayBottomSheet(
        show = show,
        modifier = Modifier,
        title = "点击查看该周课表",
        startAction = null,
        endAction = null,
        backgroundColor = BottomSheetDefaults.backgroundColor(),
        enableWindowDim = true,
        cornerRadius = BottomSheetDefaults.cornerRadius,
        sheetMaxWidth = BottomSheetDefaults.maxWidth,
        onDismissRequest = onDismiss,
        onDismissFinished = null,
        outsideMargin = BottomSheetDefaults.outsideMargin,
        insideMargin = BottomSheetDefaults.insideMargin,
        dragHandleColor = MiuixTheme.colorScheme.surface,
        defaultWindowInsetsPadding = true,
        allowDismiss = true,
        enableNestedScroll = true,
        renderInRootScaffold = true,
    ) {
        WeekPalette(
            weeks = totalWeeks.coerceAtLeast(1),
            selectedWeeks = setOf(detectedWeek),
            onToggleWeek = { week ->
                onDismiss()
                if (week != viewingWeek) {
                    onWeekSelected(week)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}
