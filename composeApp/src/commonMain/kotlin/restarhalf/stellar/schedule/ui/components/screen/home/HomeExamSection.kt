package restarhalf.stellar.schedule.ui.components.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun HomeExamSection(
    exams: List<ExamUi>,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
    onExamClick: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "考试安排", fontSize = 14.sp, color = textSecondary)
        exams.forEach { exam ->
            ExamRow(
                exam = exam,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                dividerColor = dividerColor,
                onClick = onExamClick
            )
        }
    }
}

@Immutable
data class ExamUi(
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val accentCourseName: String,
    val isEnded: Boolean,
    val isStarted: Boolean,
)

@Composable
private fun ExamRow(
    exam: ExamUi,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
    onClick: () -> Unit
) {
    val accentColor = pickCourseSubColor(exam.accentCourseName, false)
    val currentDividerColor = if (exam.isEnded) dividerColor else accentColor
    val currentPrimaryTextColor = if (exam.isEnded) textSecondary else textPrimary
    val status = when {
        exam.isEnded -> "已结束"
        exam.isStarted -> "进行中"
        else -> "未开始"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(56.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = exam.startTime, fontSize = 13.sp, color = textSecondary)
            Text(text = exam.endTime, fontSize = 13.sp, color = textSecondary)
        }
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(currentDividerColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = exam.title,
                fontSize = 16.sp,
                fontWeight = if (exam.isEnded) FontWeight.Normal else FontWeight.SemiBold,
                color = currentPrimaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = exam.location,
                fontSize = 13.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (status.isNotEmpty()) {
            Text(
                text = status,
                fontSize = 13.sp,
                color = textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}