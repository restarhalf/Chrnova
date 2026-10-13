package restarhalf.stellar.schedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.toPersistentList

private fun padTwo(n: Int): String = if (n < 10) "0$n" else "$n"


@Composable
fun TimeRangePickerBottomSheet(
    show: Boolean,
    title: String,
    initialStartHour: Int = 8,
    initialStartMinute: Int = 0,
    initialEndHour: Int = 10,
    initialEndMinute: Int = 0,
    onDismissRequest: () -> Unit,
    onConfirm: (startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit,
) {
    val hours = remember { (0..23).toList() }
    val hourItems = remember(hours) { hours.map { padTwo(it) } }

    val minutes = remember { (0..59 step 5).toList() }
    val minuteItems = remember(minutes) { minutes.map { padTwo(it) } }

    var startHourIndex by remember {
        val idx = hours.indexOf(initialStartHour.coerceIn(0, 23)).let { if (it >= 0) it else 0 }
        mutableIntStateOf(idx)
    }
    var startMinuteIndex by remember {
        val idx = minutes.indexOf(initialStartMinute.coerceIn(0, 59).let { m -> minutes.minByOrNull { kotlin.math.abs(it - m) } ?: m })
            .let { if (it >= 0) it else 0 }
        mutableIntStateOf(idx)
    }
    var endHourIndex by remember {
        val idx = hours.indexOf(initialEndHour.coerceIn(0, 23)).let { if (it >= 0) it else 0 }
        mutableIntStateOf(idx)
    }
    var endMinuteIndex by remember {
        val idx = minutes.indexOf(initialEndMinute.coerceIn(0, 59).let { m -> minutes.minByOrNull { kotlin.math.abs(it - m) } ?: m })
            .let { if (it >= 0) it else 0 }
        mutableIntStateOf(idx)
    }

    WheelPickerBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val sh = hours.getOrNull(startHourIndex) ?: 8
            val sm = minutes.getOrNull(startMinuteIndex) ?: 0
            val eh = hours.getOrNull(endHourIndex) ?: 10
            val em = minutes.getOrNull(endMinuteIndex) ?: 0
            onConfirm(sh, sm, eh, em)
        },
    ) {
        WheelPickerRow(
            columns =
                listOf(
                    WheelPickerColumnState(
                        items = hourItems.toPersistentList(),
                        selectedIndex = startHourIndex,
                        onSelectedIndexChange = { startHourIndex = it },
                    ),
                    WheelPickerColumnState(
                        items = minuteItems.toPersistentList(),
                        selectedIndex = startMinuteIndex,
                        onSelectedIndexChange = { startMinuteIndex = it },
                    ),
                    WheelPickerColumnState(
                        items = listOf("-").toPersistentList(),
                        selectedIndex = 0,
                        onSelectedIndexChange = {},
                        visibleCount = 1,
                    ),
                    WheelPickerColumnState(
                        items = hourItems.toPersistentList(),
                        selectedIndex = endHourIndex,
                        onSelectedIndexChange = { endHourIndex = it },
                    ),
                    WheelPickerColumnState(
                        items = minuteItems.toPersistentList(),
                        selectedIndex = endMinuteIndex,
                        onSelectedIndexChange = { endMinuteIndex = it },
                    ),
                ),
        )
    }
}
