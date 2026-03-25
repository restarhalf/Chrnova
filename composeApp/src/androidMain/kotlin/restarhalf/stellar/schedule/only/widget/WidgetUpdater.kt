package restarhalf.stellar.schedule.only.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object WidgetUpdater {
    suspend fun refreshAll(context: Context) =
        withContext(Dispatchers.Default) {
            TodaySmallWidget().updateAll(context)
            TodayLargeWidget().updateAll(context)
        }

    suspend fun refreshLargeOnly(context: Context) =
        withContext(Dispatchers.Default) {
            TodayLargeWidget().updateAll(context)
        }

    suspend fun refreshSmallOnly(context: Context) =
        withContext(Dispatchers.Default) {
            TodaySmallWidget().updateAll(context)
        }
}
