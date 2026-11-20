package restarhalf.stellar.schedule.ui.components.screen.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.components.LocalLiquidGlassBackdrop
import restarhalf.stellar.schedule.ui.components.WeekPalette
import restarhalf.stellar.schedule.ui.components.sheetContentBottomInset
import top.yukonga.miuix.kmp.overlay.BlurBottomSheet

@Composable
fun WeekPickerSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    totalWeeks: Int,
    viewingWeek: Int,
    detectedWeek: Int,
    onWeekSelected: (Int) -> Unit,
) {
    BlurBottomSheet(
        show = show,
        title = "点击查看该周课表",
        dimBackground = true,
        glassBackdrop = LocalLiquidGlassBackdrop.current,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(58.dp))
            WeekPalette(
                modifier = Modifier.padding(horizontal = 24.dp),
                weeks = totalWeeks.coerceAtLeast(1),
                selectedWeeks = setOf(detectedWeek),
                onToggleWeek = { week ->
                    onDismiss()
                    if (week != viewingWeek) {
                        onWeekSelected(week)
                    }
                }
            )

            Spacer(modifier = Modifier.height(sheetContentBottomInset()))
        }
    }
}
