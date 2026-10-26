package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import restarhalf.stellar.schedule.core.text.uniqueKeys

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
