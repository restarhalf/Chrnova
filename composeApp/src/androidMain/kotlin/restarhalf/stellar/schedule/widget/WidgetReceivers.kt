package restarhalf.stellar.schedule.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import java.util.concurrent.ConcurrentHashMap

private const val ACTION_MIUI_APPWIDGET_UPDATE = "miui.appwidget.action.APPWIDGET_UPDATE"

private const val MIUI_REFRESH_THROTTLE_MS = 2_000L

private val miuiRefreshAtMs = ConcurrentHashMap<String, Long>()

abstract class TodayWidgetReceiver : GlanceAppWidgetReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MIUI_APPWIDGET_UPDATE) {
            if (!acceptMiuiRefresh(context)) return
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            if (!intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
                val ids =
                    AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(ComponentName(context, javaClass))
                if (ids.isEmpty()) return
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
        }
        super.onReceive(context, intent)
    }

    private fun acceptMiuiRefresh(context: Context): Boolean {
        val key = ComponentName(context, javaClass).flattenToShortString()
        val now = SystemClock.elapsedRealtime()
        val previous = miuiRefreshAtMs.put(key, now)
        return previous == null || now - previous >= MIUI_REFRESH_THROTTLE_MS
    }
}

class TodaySmallWidgetReceiver : TodayWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodaySmallWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshController.refreshAsync(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshController.scheduleFallback(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetRefreshController.refreshAsync(context)
    }
}

class TodayLargeWidgetReceiver : TodayWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayLargeWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshController.refreshAsync(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshController.scheduleFallback(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetRefreshController.refreshAsync(context)
    }
}
