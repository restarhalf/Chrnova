package restarhalf.stellar.schedule.ui.components.screen.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
fun WeekHeaderRow(
    ui: ScheduleViewModel.WeekHeaderUi,
    primary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier,
) {

    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(36.dp))

        ui.days.zip(ui.dates).forEachIndexed { index, (day, date) ->
            val isToday = ui.todayIndex == index

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) primary else textSecondary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))


                Box(
                    modifier =
                        Modifier
                            .background(
                                color = if (isToday) primary.copy(0.7f) else Color.Transparent,
                                shape = CircleShape,
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = date,
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) Color.White else textSecondary,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
        }
    }
}
