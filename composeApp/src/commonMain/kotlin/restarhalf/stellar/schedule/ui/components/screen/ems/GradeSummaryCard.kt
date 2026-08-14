package restarhalf.stellar.schedule.ui.components.screen.ems

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GradeSummaryCard(
    modifier: Modifier = Modifier,
    summary: GradeViewModel.GradeSummary,
) {
    val animProgress = remember { Animatable(0f) }
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(Unit) {
        animProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 300))
    }

    AppCard(
        modifier = modifier.graphicsLayer {
            alpha = animProgress.value
            translationY = 50f * (1f - animProgress.value)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryItem(
                value = summary.earnedCredits.ifBlank { "--" },
                label = "已修学分",
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(colors.outline)
            )
            SummaryItem(
                value = summary.totalGradePoints.ifBlank { "--" },
                label = "总绩点",
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(colors.outline)
            )
            SummaryItem(
                value = summary.averageCreditGradePoint.ifBlank { "--" },
                label = "平均绩点",
                valueColor = colors.primary,
            )
        }
    }
}

@Composable
private fun SummaryItem(
    value: String,
    label: String,
    valueColor: androidx.compose.ui.graphics.Color = MiuixTheme.colorScheme.onSurface,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            style = MiuixTheme.textStyles.title4,
            color = valueColor,
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = colors.onSurfaceVariantSummary,
        )
    }
}
