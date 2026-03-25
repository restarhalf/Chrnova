package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WeekPalette(
    modifier: Modifier = Modifier,
    weeks: Int = 20,
    selectedWeeks: Set<Int>,
    disabledWeeks: Set<Int> = emptySet(),
    onToggleWeek: (Int) -> Unit,
    columns: Int = 6,
    itemHeight: Dp = 30.dp,
    cornerRadius: Dp = 8.dp,
    horizontalSpacing: Dp = 6.dp,
    verticalSpacing: Dp = 6.dp,
) {
    val selectedBg = MiuixTheme.colorScheme.primary
    val unselectedBg = MiuixTheme.colorScheme.secondary
    val selectedText = MiuixTheme.colorScheme.onPrimary
    val unselectedText = Color.Unspecified.copy(0.8f)
    val notEnableText = MiuixTheme.colorScheme.onSecondary.copy(0.4f)

    val rows = (1..weeks).toList().chunked(columns)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        rows.forEach { rowWeeks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            ) {
                for (col in 0 until columns) {
                    val week = rowWeeks.getOrNull(col)
                    if (week == null) {
                        Spacer(modifier = Modifier.weight(1f).height(itemHeight))
                        continue
                    }

                    val selected = selectedWeeks.contains(week)
                    val enabled = !disabledWeeks.contains(week)
                    val bg =
                        if (!enabled) {
                            unselectedBg.copy(alpha = 0.4f)
                        } else {
                            if (selected) selectedBg else unselectedBg
                        }
                    val fg =
                        if (!enabled) {
                            notEnableText
                        } else {
                            if (selected) selectedText else unselectedText
                        }
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .height(itemHeight)
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(bg)
                                .clickable(enabled = enabled) { onToggleWeek(week) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = week.toString(),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = fg,
                        )
                    }
                }
            }
        }
    }
}
