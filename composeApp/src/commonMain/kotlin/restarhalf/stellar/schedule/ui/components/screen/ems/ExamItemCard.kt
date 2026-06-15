package restarhalf.stellar.schedule.ui.components.screen.ems

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 考试项目卡片组件
 * 
 * 在考试安排列表中显示单个考试信息，包括：
 * - 课程名称
 * - 考试日期和时间
 * - 考试地点
 * - 座位号
 * - 备注信息
 * 
 * @param modifier Modifier修饰符
 * @param card 考试卡片UI数据
 */
@Composable
fun ExamItemCard(
    modifier: Modifier = Modifier,
    card: ExaminationViewModel.ExamCardUi,
    onClick: () -> Unit = {},
) {
    val animProgress = remember { Animatable(0f) }
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val exam = card.exam
    val accentColor = pickCourseSubColor(card.title.ifBlank { exam.courseNumber }, false)
    LaunchedEffect(Unit) {
        animProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 300))
    }
    AppCard(
        modifier =
            modifier
                .graphicsLayer {
                    alpha = animProgress.value
                    translationY = 50f * (1f - animProgress.value)
                }
                .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier =
                    Modifier.align(Alignment.CenterVertically)
                        .clip(CircleShape)
                        .background(accentColor)
                        .width(4.dp)
                        .fillMaxHeight(0.9f)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = card.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                    Text(
                        text = card.dateText,
                        fontSize = 12.sp,
                        color = summary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.timeText,
                        fontSize = 12.sp,
                        color = summary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.locationText,
                        fontSize = 12.sp,
                        color = summary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.seatText,
                        fontSize = 12.sp,
                        color = summary
                    )
                val remarkText = card.remarkText
                if (remarkText != null) {
                    Box(
                        modifier =
                            Modifier.clip(RoundedCornerShape(6.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .align(Alignment.End)
                    ) {
                        Text(text = remarkText, fontSize = 11.sp, color = summary)
                    }
                }
            }
        }
    }
}