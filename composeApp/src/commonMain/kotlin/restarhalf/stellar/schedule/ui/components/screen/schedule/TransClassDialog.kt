package restarhalf.stellar.schedule.ui.components.screen.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.components.SectionRangePickerBottomSheet
import restarhalf.stellar.schedule.ui.components.WeekPickerBottomSheet
import restarhalf.stellar.schedule.ui.components.WeekdayPickerBottomSheet
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.DialogDefaults
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TransClassDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    totalWeeks: Int,
    onTotalWeeksChange: (Int) -> Unit,
    newClassRoom: String,
    onNewClassRoomChange: (String) -> Unit,
    dayOfWeek: Int,
    onDayOfWeekChange: (Int) -> Unit,
    startSection: Int,
    endSection: Int,
    onSectionRangeChange: (Int, Int) -> Unit,
    onTrans: () -> Unit
) {

    val showWeekdayPicker = remember { mutableStateOf(false) }
    val showWeekPicker = remember { mutableStateOf(false) }
    val showSectionPicker = remember { mutableStateOf(false) }

    val weekdayText =
        remember(dayOfWeek) {
            when (dayOfWeek) {
                1 -> "周一"
                2 -> "周二"
                3 -> "周三"
                4 -> "周四"
                5 -> "周五"
                6 -> "周六"
                7 -> "周日"
                else -> "周一"
            }
        }

    SuperDialog(
        show = show,
        modifier = Modifier,
        title = "调课",
        titleColor = DialogDefaults.titleColor(),
        summary = "在老师调课时使用",
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = null,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        renderInRootScaffold = true,
        content = {
            if (showWeekdayPicker.value) {
                WeekdayPickerBottomSheet(
                    show = showWeekdayPicker,
                    title = "调课星期",
                    initialDayOfWeek = dayOfWeek,
                    onConfirm = { newDayOfWeek ->
                        onDayOfWeekChange(newDayOfWeek)
                        showWeekdayPicker.value = false
                    },
                )
            }

            if (showWeekPicker.value) {
                WeekPickerBottomSheet(
                    show = showWeekPicker,
                    title = "调课周数",
                    initialWeek = totalWeeks,
                    weekRange = 1..20,
                    onConfirm = { week: Int ->
                        onTotalWeeksChange(week)
                        showWeekPicker.value = false
                    },
                )
            }

            if (showSectionPicker.value) {
                SectionRangePickerBottomSheet(
                    show = showSectionPicker,
                    title = "调课节数",
                    sectionRange = 1..12,
                    initialStartSection = startSection,
                    initialEndSection = endSection,
                    onConfirm = { newStartSection, newEndSection ->
                        onSectionRangeChange(newStartSection, newEndSection)
                        showSectionPicker.value = false
                    },
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    TextField(
                        value = newClassRoom,
                        onValueChange = onNewClassRoomChange,
                        label = "新教室",
                    )
                }

                item {
                    SuperArrow(
                        title = "调课星期",
                        summary = weekdayText,
                        onClick = { showWeekdayPicker.value = true })
                }

                item {
                    SuperArrow(
                        title = "调课周数",
                        summary = "第${totalWeeks}周",
                        onClick = { showWeekPicker.value = true })
                }

                item {
                    SuperArrow(
                        title = "调课节数",
                        summary = "第${startSection}-${endSection}节",
                        onClick = { showSectionPicker.value = true },
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(modifier = Modifier.weight(1f), onClick = onDismiss) {
                            Text(text = "取消")
                        }

                        Spacer(modifier = Modifier.size(16.dp))

                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = onTrans,
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text(text = "确定", color = MiuixTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        })
}