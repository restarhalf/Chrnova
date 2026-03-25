package restarhalf.stellar.schedule.ui.components.screen.grade

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GradeItemCard(
    modifier: Modifier = Modifier,
    card: GradeViewModel.GradeCardUi,
    onClick: () -> Unit,
) {
    val animProgress = remember { Animatable(0f) }
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val grade = card.grade
    val accentColor = pickCourseSubColor(grade.courseName.ifBlank { grade.courseCode }, false)

    LaunchedEffect(Unit) {
        animProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 300))
    }

    AppCard(
        modifier =
            modifier.graphicsLayer {
                alpha = animProgress.value
                translationY = 50f * (1f - animProgress.value)
            }.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(accentColor)
                            .width(4.dp)
                            .height(30.dp)
                            .align(Alignment.CenterVertically)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (card.isRetakeExam) {
                            Box(
                                modifier =
                                    Modifier.padding(top = 2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFCB065).copy(0.8f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "补考",
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSecondary
                                )
                            }
                        }
                        Text(
                            text = card.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                    }
                    Text(
                        text = card.subtitle,
                        fontSize = 12.sp,
                        color = summary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = card.scoreText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Text(text = card.attrText, fontSize = 12.sp, color = summary)
                }
            }
        }
    }
}
