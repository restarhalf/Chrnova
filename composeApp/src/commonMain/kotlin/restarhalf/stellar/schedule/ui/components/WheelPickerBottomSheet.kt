package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 滚轮选择器底部弹窗
 * 
 * 提供通用的滚轮选择器弹窗框架，支持：
 * - 自定义内容
 * - 标题显示
 * - 确认/取消操作
 * - 滚动到选中项动画
 * - 触觉反馈
 * 
 * @param show 是否显示
 * @param title 标题
 * @param onDismissRequest 关闭回调
 * @param onConfirm 确认回调
 * @param content 自定义内容
 */
@Composable
fun WheelPickerBottomSheet(
    show: MutableState<Boolean>,
    title: String,
    onDismissRequest: () -> Unit = { show.value = false },
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    WindowBottomSheet(
        show = show.value,
        modifier = Modifier,
        title = title,
        startAction = null,
        endAction = null,
        backgroundColor = BottomSheetDefaults.backgroundColor(),
        enableWindowDim = true,
        cornerRadius = BottomSheetDefaults.cornerRadius,
        sheetMaxWidth = BottomSheetDefaults.maxWidth,
        onDismissRequest = onDismissRequest,
        onDismissFinished = null,
        outsideMargin = BottomSheetDefaults.outsideMargin,
        insideMargin = BottomSheetDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        dragHandleColor = MiuixTheme.colorScheme.surface,
        allowDismiss = false,
        enableNestedScroll = true,
        content = {
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
                    Text(text = "确定", color = MiuixTheme.colorScheme.onPrimary)
                }
            }
        })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 44.dp,
) {
    val clampedSelectedIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    visibleCount / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = clampedSelectedIndex)

    WheelPickerColumn(
        items = items,
        selectedIndex = clampedSelectedIndex,
        onSelectedIndexChange = onSelectedIndexChange,
        modifier = modifier,
        visibleCount = visibleCount,
        itemHeight = itemHeight,
        listState = listState,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 44.dp,
    listState: LazyListState,
) {
    val paddingItemCount = visibleCount / 2
    val haptic = LocalHapticFeedback.current
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val onSelectedIndexChangeState by rememberUpdatedState(onSelectedIndexChange)

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            } else {
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closest =
                    visibleItems.minBy { info ->
                        kotlin.math.abs((info.offset + info.size / 2) - viewportCenter)
                    }
                closest.index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            }
        }
    }

    LaunchedEffect(listState, items.size, paddingItemCount) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect { idx ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectedIndexChangeState(idx)
            }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount).fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * paddingItemCount),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items) { index, value ->
                val isSelected = index == centerIndex
                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value,
                        modifier = Modifier.alpha(if (isSelected) 1f else 0.22f),
                        textAlign = TextAlign.Center,
                        color =
                            if (isSelected) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.onSurface,
                        fontSize = if (isSelected) 30.sp else 20.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(itemHeight * 2)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to MiuixTheme.colorScheme.background,
                            1f to Color.Transparent,
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(itemHeight * 2)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to MiuixTheme.colorScheme.background,
                        ),
                    ),
        )
    }
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
        columns.forEach { col ->
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

data class WheelPickerColumnState(
    val items: List<String>,
    val selectedIndex: Int,
    val onSelectedIndexChange: (Int) -> Unit,
    val visibleCount: Int = 5,
    val itemHeight: Dp = 44.dp,
)
