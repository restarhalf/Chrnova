package restarhalf.stellar.schedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private val WEEKDAY_ITEMS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@Composable
fun WeekdayPickerBottomSheet(
    show: Boolean,
    title: String,
    initialDayOfWeek: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val days = remember { (1..7).toList() }

    var selectedIndex by remember {
        val idx = days.indexOf(initialDayOfWeek).let { if (it >= 0) it else 0 }
        mutableIntStateOf(idx)
    }

    WheelPickerBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val day = days.getOrNull(selectedIndex) ?: days.firstOrNull() ?: initialDayOfWeek
            onConfirm(day)
        },
    ) {
        WheelPickerColumn(
            items = WEEKDAY_ITEMS,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { selectedIndex = it },
        )
    }
}
