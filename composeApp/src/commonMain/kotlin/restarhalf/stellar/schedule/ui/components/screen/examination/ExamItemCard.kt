package restarhalf.stellar.schedule.ui.components.screen.examination

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import top.yukonga.miuix.kmp.theme.miuixCapsuleShape
import top.yukonga.miuix.kmp.theme.miuixShape

@Composable
fun ExamItemCard(
    modifier: Modifier = Modifier,
    card: ExaminationViewModel.ExamCardUi,
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
            modifier.graphicsLayer {
                alpha = animProgress.value
                translationY = 50f * (1f - animProgress.value)
            }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier =
                    Modifier.align(Alignment.CenterVertically)
                        .clip(miuixCapsuleShape())
                        .background(accentColor)
                        .width(4.dp)
                        .height(50.dp)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                }


                val remarkText = card.remarkText
                if (remarkText != null) {
                    Box(
                        modifier =
                            Modifier.clip(miuixShape(6.dp))
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