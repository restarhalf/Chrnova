package restarhalf.stellar.schedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 课程表格子高度选择弹窗
 *
 * 与 [WeekPickerBottomSheet] 同构：滚轮选择一个 Int 值，确定后才提交。
 */
@Composable
fun RowHeightPickerBottomSheet(
    show: Boolean,
    title: String,
    initialHeightDp: Int,
    heightRange: IntRange,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val heights = remember(heightRange) { heightRange.toList() }
    val heightItems = remember(heights) { heights.map { it.toString() } }

    var selectedIndex by remember {
        val idx = heights.indexOf(initialHeightDp).let { if (it >= 0) it else 0 }
        mutableIntStateOf(idx)
    }

    WheelPickerBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val height = heights.getOrNull(selectedIndex) ?: heights.firstOrNull() ?: initialHeightDp
            onConfirm(height)
        },
    ) {
        WheelPickerColumn(
            items = heightItems,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { selectedIndex = it },
        )
    }
}
