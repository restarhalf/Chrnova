package restarhalf.stellar.schedule.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TodaySmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodaySmallWidget()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshController.refreshAndScheduleAsync(context)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshController.scheduleIfNeeded(context)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetRefreshController.refreshAndScheduleAsync(context)
    }
}

class TodayLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayLargeWidget()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshController.refreshAndScheduleAsync(context)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshController.scheduleIfNeeded(context)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetRefreshController.refreshAndScheduleAsync(context)
    }
}
