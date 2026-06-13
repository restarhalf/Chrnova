package restarhalf.stellar.schedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 计算指定年月的天数
 * 
 * @param year 年份
 * @param month 月份（1-12）
 * @return 该月天数
 */
private fun daysInMonth(year: Int, month: Int): Int {
    val firstDay = LocalDate(year, month, 1)
    val nextMonthFirstDay =
        if (month == 12) {
            LocalDate(year + 1, 1, 1)
        } else {
            LocalDate(year, month + 1, 1)
        }
    return firstDay.daysUntil(nextMonthFirstDay)
}

/**
 * 日期选择器底部弹窗
 * 
 * 提供年月滚轮选择器，用于选择日期。
 * 
 * @param show 是否显示
 * @param title 标题
 * @param initialDate 初始日期
 * @param yearRange 年份范围
 * @param onDismissRequest 关闭回调
 * @param onConfirm 确认回调
 */
@OptIn(ExperimentalTime::class)
@Composable
fun DatePickerBottomSheet(
    show: MutableState<Boolean>,
    title: String,
    initialDate: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    yearRange: IntRange = (initialDate.year - 10)..(initialDate.year + 10),
    onDismissRequest: () -> Unit = { show.value = false },
    onConfirm: (LocalDate) -> Unit,
) {
    val years = remember(yearRange) { yearRange.toList() }
    val yearItems = remember(years) { years.map { "$it" } }
    val monthItems = remember { (1..12).map { "$it" } }

    var selectedYearIndex by remember {
        val idx = years.indexOf(initialDate.year).let { if (it >= 0) it else 0 }
        mutableIntStateOf(idx)
    }
    var selectedMonthIndex by remember {
        mutableIntStateOf(initialDate.month.ordinal.coerceIn(0, 11))
    }

    val selectedYear = years.getOrNull(selectedYearIndex) ?: years.firstOrNull() ?: initialDate.year
    val selectedMonth = (selectedMonthIndex + 1).coerceIn(1, 12)

    val maxDay =
        remember(selectedYear, selectedMonth) {
            daysInMonth(selectedYear, selectedMonth)
        }
    val dayItems = remember(selectedYear, selectedMonth) { (1..maxDay).map { "$it" } }

    var selectedDayIndex by remember {
        val idx = (initialDate.day - 1).coerceIn(0, (maxDay - 1).coerceAtLeast(0))
        mutableIntStateOf(idx)
    }

    LaunchedEffect(maxDay) {
        selectedDayIndex = selectedDayIndex.coerceIn(0, (maxDay - 1).coerceAtLeast(0))
    }

    WheelPickerBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val day = (selectedDayIndex + 1).coerceIn(1, maxDay)
            onConfirm(LocalDate(selectedYear, selectedMonth, day))
        },
    ) {
        WheelPickerRow(
            columns =
                listOf(
                    WheelPickerColumnState(
                        items = yearItems,
                        selectedIndex = selectedYearIndex,
                        onSelectedIndexChange = { selectedYearIndex = it },
                    ),
                    WheelPickerColumnState(
                        items = monthItems,
                        selectedIndex = selectedMonthIndex,
                        onSelectedIndexChange = { selectedMonthIndex = it },
                    ),
                    WheelPickerColumnState(
                        items = dayItems,
                        selectedIndex = selectedDayIndex,
                        onSelectedIndexChange = { selectedDayIndex = it },
                    ),
                ),
        )
    }
}
