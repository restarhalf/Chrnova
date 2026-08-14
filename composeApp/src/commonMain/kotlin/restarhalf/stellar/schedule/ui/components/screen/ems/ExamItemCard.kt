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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.theme.StatusColors
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
    val colors = MiuixTheme.colorScheme
    val summary = colors.onSurfaceVariantSummary
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
        Box(modifier = Modifier.fillMaxWidth()) {
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
                        style = MiuixTheme.textStyles.body1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                        Text(
                            text = card.dateText,
                            style = MiuixTheme.textStyles.footnote1,
                            color = summary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = card.timeText,
                            style = MiuixTheme.textStyles.footnote1,
                            color = summary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = card.locationText,
                            style = MiuixTheme.textStyles.footnote1,
                            color = summary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = card.seatText,
                            style = MiuixTheme.textStyles.footnote1,
                            color = summary
                        )
                    val remarkText = card.remarkText
                    if (remarkText != null) {
                        Box(
                            modifier =
                                Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(colors.surfaceContainerHigh)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .align(Alignment.End)
                        ) {
                            Text(text = remarkText, style = MiuixTheme.textStyles.footnote2, color = summary)
                        }
                    }
                }
            }

            if (card.isEnded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(colors.surface.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusColors.neutral.copy(0.8f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "已结束",
                            color = Color.White,
                            style = MiuixTheme.textStyles.footnote2
                        )
                    }
                }
            }
        }
    }
}