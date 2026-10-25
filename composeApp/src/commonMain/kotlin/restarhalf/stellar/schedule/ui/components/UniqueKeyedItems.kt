package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import restarhalf.stellar.schedule.core.text.uniqueKeys

/**
 * 与 `items` 相同，但按 [keyOf] 为重复项生成唯一 key 兜底。
 *
 * key 必须与 [items] 在同一个列表作用域里计算，不要在外层 `remember` 好再传进来。
 * LazyColumn 的 content lambda 在测量阶段执行，可能晚于最后一次重组；此时它读到的
 * 状态（例如 `val uiState by ...` 的委托 getter）已经是新值，而外层 remember 的结果
 * 还是旧的。两者错位就会 `keys[index]` 越界崩溃（典型表现是 index 0）。
 */
fun <T> LazyListScope.itemsWithUniqueKeys(
    items: List<T>,
    keyOf: (T) -> String,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    val keys = items.uniqueKeys(keyOf)
    itemsIndexed(
        items = items,
        key = { index, _ -> keys[index] },
    ) { _, item -> itemContent(item) }
}
