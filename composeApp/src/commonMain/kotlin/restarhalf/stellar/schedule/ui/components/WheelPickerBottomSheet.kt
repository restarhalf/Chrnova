package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.BlurBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WheelPickerBottomSheet(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    BlurBottomSheet(
        show = show,
        title = title,
        dimBackground = true,
        // 选择器弹窗接柔光玻璃：采「壁纸 + 本页内容」录制层，弹窗后透出的是它盖住的那一页
        glassBackdrop = LocalLiquidGlassBackdrop.current,
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(58.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                content()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onDismissRequest,
                ) {
                    Text(text = "取消")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(text = "确定", color = colors.onPrimary)
                }
            }
            Spacer(modifier = Modifier.height(sheetContentBottomInset()))
        }
    }
}

/**
 * 滚轮选择列，直接复用 miuix 的 [NumberPicker]：
 * 拖拽/惯性/吸附、跨项震动、缩放与淡化都交给它，和 Nexio 的手感保持一致。
 */
@Composable
fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 44.dp,
) {
    if (items.isEmpty()) {
        Spacer(modifier = modifier.height(itemHeight * visibleCount))
        return
    }

    val clampedSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val currentOnSelectedIndexChange by rememberUpdatedState(onSelectedIndexChange)

    // 单列分隔符（如时间范围里的 "-"）或不足三项时没有滚动可言，画一个静态文本占位。
    if (items.size == 1 || visibleCount < 3) {
        Box(
            modifier = modifier.height(itemHeight * visibleCount.coerceAtLeast(1)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = items[clampedSelectedIndex],
                style = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.SemiBold),
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
        return
    }

    NumberPicker(
        value = clampedSelectedIndex,
        onValueChange = { currentOnSelectedIndexChange(it) },
        modifier = modifier,
        range = items.indices,
        label = { items[it] },
        visibleItemCount = visibleCount,
        colors = NumberPickerDefaults.colors(
            selectedTextColor = MiuixTheme.colorScheme.primary,
        ),
        itemHeight = itemHeight,
    )
}

@Composable
fun WheelPickerRow(
    columns: List<WheelPickerColumnState>,
    modifier: Modifier = Modifier,
    columnSpacing: Dp = 12.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(columnSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEachIndexed { index, col ->
            val effectiveKey = col.key.ifBlank { "col_$index" }
            key(effectiveKey) {
                WheelPickerColumn(
                    items = col.items,
                    selectedIndex = col.selectedIndex,
                    onSelectedIndexChange = col.onSelectedIndexChange,
                    modifier = Modifier.weight(1f),
                    visibleCount = col.visibleCount,
                    itemHeight = col.itemHeight,
                )
            }
        }
    }
}

@Stable
data class WheelPickerColumnState(
    val key: String = "",
    val items: ImmutableList<String>,
    val selectedIndex: Int,
    val onSelectedIndexChange: (Int) -> Unit,
    val visibleCount: Int = 5,
    val itemHeight: Dp = 44.dp,
)
