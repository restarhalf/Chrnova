package restarhalf.stellar.schedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun SectionRangePickerBottomSheet(
    show: MutableState<Boolean>,
    title: String,
    sectionRange: IntRange,
    initialStartSection: Int,
    initialEndSection: Int,
    onDismissRequest: () -> Unit = { show.value = false },
    onConfirm: (startSection: Int, endSection: Int) -> Unit,
) {
    val sections = remember(sectionRange) { sectionRange.toList() }
    val sectionItems = remember(sections) { sections.map { "${it}节" } }

    var startIndex by remember {
        val idx = sections.indexOf(initialStartSection).let { if (it >= 0) it else 0 }
        mutableIntStateOf(idx)
    }
    var endIndex by remember {
        val idx = sections.indexOf(initialEndSection).let { if (it >= 0) it else startIndex }
        mutableIntStateOf(idx)
    }

    LaunchedEffect(startIndex) { if (endIndex < startIndex) endIndex = startIndex }

    WheelPickerBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val startSection =
                sections.getOrNull(startIndex) ?: sections.firstOrNull() ?: initialStartSection
            val endSection = sections.getOrNull(endIndex) ?: startSection
            onConfirm(startSection, endSection)
        },
    ) {
        WheelPickerRow(
            columns =
                listOf(
                    WheelPickerColumnState(
                        items = sectionItems,
                        selectedIndex = startIndex,
                        onSelectedIndexChange = { idx -> startIndex = idx },
                    ),
                    WheelPickerColumnState(
                        items = sectionItems,
                        selectedIndex = endIndex,
                        onSelectedIndexChange = { idx -> endIndex = idx },
                    ),
                ),
        )
    }
}
