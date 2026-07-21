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
import top.yukonga.miuix.kmp.squircle.squircleSurface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 周次选择器组件
 * 
 * 网格状的周次选择器，支持多选、禁用状态。
 * 用于实验课编辑时选择上课周次。
 * 
 * @param modifier Modifier修饰符
 * @param weeks 总周数
 * @param selectedWeeks 已选中的周次集合
 * @param disabledWeeks 禁用的周次集合（有冲突的周次）
 * @param onToggleWeek 切换周次选中状态的回调
 * @param columns 每行列数
 * @param itemHeight 每个周次项的高度
 * @param cornerRadius 圆角半径
 * @param horizontalSpacing 水平间距
 * @param verticalSpacing 垂直间距
 */
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
    // 定义颜色
    val selectedBg = MiuixTheme.colorScheme.primary
    val unselectedBg = MiuixTheme.colorScheme.secondary
    val selectedText = MiuixTheme.colorScheme.onPrimary
    val unselectedText = Color.Unspecified.copy(0.8f)
    val notEnableText = MiuixTheme.colorScheme.onSecondary.copy(0.4f)

    // 按列数分组
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
                                .squircleSurface(bg, cornerRadius)
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
