package restarhalf.stellar.schedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun WeekPickerBottomSheet(
    show: Boolean,
    title: String,
    initialWeek: Int,
    weekRange: IntRange,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val weeks = remember(weekRange) { weekRange.toList() }
    val weekItems = remember(weeks) { weeks.map { it.toString() } }

    var selectedIndex by remember {
        val idx = weeks.indexOf(initialWeek).let { if (it >= 0) it else 0 }
        mutableIntStateOf(idx)
    }

    WheelPickerBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val week = weeks.getOrNull(selectedIndex) ?: weeks.firstOrNull() ?: initialWeek
            onConfirm(week)
        },
    ) {
        WheelPickerColumn(
            items = weekItems,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { selectedIndex = it },
        )
    }
}
