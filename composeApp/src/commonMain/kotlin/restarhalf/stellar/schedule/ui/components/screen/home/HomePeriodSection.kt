package restarhalf.stellar.schedule.ui.components.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodRenderRowsUseCase
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.miuixCapsuleShape

private typealias PeriodRowRenderUi = BuildHomePeriodRenderRowsUseCase.RowRenderUi

@Composable
fun HomePeriodSection(
    title: String,
    rows: List<PeriodRowRenderUi>,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = title, fontSize = 14.sp, color = textSecondary)
        rows.forEach { row ->
            val rowUi = row.rowUi
            val accentColor = pickCourseSubColor(row.accentCourseName, false)
            val currentDividerColor = if (rowUi.isPastOrEmpty) dividerColor else accentColor
            val currentPrimaryTextColor = if (rowUi.isPastOrEmpty) textSecondary else textPrimary

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(56.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = row.timeRange.first, fontSize = 13.sp, color = textSecondary)
                    Text(text = row.timeRange.second, fontSize = 13.sp, color = textSecondary)
                }
                Box(
                    modifier =
                        Modifier
                            .width(4.dp)
                            .height(36.dp)
                            .background(currentDividerColor, miuixCapsuleShape())
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = rowUi.primaryText,
                        fontSize = 16.sp,
                        fontWeight = if (rowUi.isPastOrEmpty) FontWeight.Normal else FontWeight.SemiBold,
                        color = currentPrimaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = rowUi.secondaryText,
                        fontSize = 13.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (row.status != null) {
                    Text(
                        text = row.status,
                        fontSize = 13.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}