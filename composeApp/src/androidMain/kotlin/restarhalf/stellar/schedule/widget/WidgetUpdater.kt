package restarhalf.stellar.schedule.widget

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 刷新全部已放置的小组件（RemoteViews 版：按 provider 逐个广播）。 */
internal object WidgetUpdater {
    suspend fun refreshAll(context: Context) =
        withContext(Dispatchers.Default) {
            WidgetUpdateCache.updateInstalledWidgets(context)
        }
}
