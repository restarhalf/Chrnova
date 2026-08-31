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

/**
 * 周次选择底部弹窗（点击课表顶栏弹出）
 *
 * 网格内容复用 [WeekPalette]，单选语义：点击某周即关闭并跳转。
 * 当前实际所处周以选中态高亮。
 *
 * @param show 是否显示
 * @param onDismiss 关闭回调
 * @param totalWeeks 学期总周数
 * @param viewingWeek 当前正在查看的周次
 * @param detectedWeek 实际所处周次（高亮标注）
 * @param onWeekSelected 选中某周回调
 */
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
