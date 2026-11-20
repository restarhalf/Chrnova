/** 课程表桌面小组件提供者 (Pad 2×1 版) */
package restarhalf.stellar.schedule.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.data.wallpaper.WidgetWallpaperStore

class CourseWidgetProviderPad : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "restarhalf.stellar.schedule.UPDATE_WIDGET_PAD"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, CourseWidgetProviderPad::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        val ids = appWidgetIds.copyOf()
        WidgetRefreshController.scope.launch {
            try {
                ids.forEach { updateWidget(appContext, appWidgetManager, it) }
            } catch (e: Exception) {
                logWidgetFailure("CourseWidgetPad", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, CourseWidgetProviderPad::class.java)
            )
            if (appWidgetIds.isNotEmpty()) onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val key = WidgetWallpaperStore.KEY_TODAY
        val dark = WidgetWallpaperRender.resolveDark(context, key)
        val data = WidgetCourseResolver.load(dark)
        val currentMinutes = data.nowMinutes
        val showTomorrow = data.showTomorrow()

        val resolution = if (showTomorrow) data.tomorrow else data.today
        val titleText = if (showTomorrow) {
            "${WidgetStrings.TOMORROW_COURSES} / ${WidgetStrings.weekdayLabel(resolution.calendarDayOfWeek)}"
        } else {
            "${WidgetStrings.TODAY} / ${WidgetStrings.weekdayLabel(resolution.calendarDayOfWeek)}"
        }
        val weekText = weekTextOf(data, resolution)

        val displayCourses = if (showTomorrow) {
            resolution.courses.take(2)
        } else {
            data.today.courses.filter {
                WidgetTimeSpan.endOffsetMinutes(it.startMin, it.endMin) > currentMinutes
            }.take(2)
        }

        val slotSig = displayCourses.joinToString("|") { c ->
            val remaining = if (showTomorrow) null else remainingMinutes(c, currentMinutes)
            "${c.id}:${c.name}:${c.start}-${c.end}:${c.color}:${remaining ?: -1}:${buildCourseInfo(c, true)}"
        }
        val emptyText = if (displayCourses.isEmpty()) {
            emptyTextOf(data, resolution, showTomorrow)
        } else {
            ""
        }
        val signature = "${dark}|${WidgetDeps.display.getWidgetPaddingMode()}|$titleText|$weekText|$slotSig|$emptyText|" +
            WidgetWallpaperRender.signaturePart(key)
        if (WidgetUpdateCache.shouldSkip("course_pad_$appWidgetId", signature)) return

        val views = RemoteViews(context.packageName, R.layout.widget_course_reminder_standard)
        WidgetWallpaperRender.apply(
            views = views,
            context = context,
            key = key,
            appWidgetId = appWidgetId,
            imageId = R.id.widget_wallpaper,
            containerId = R.id.widget_container,
            normalBackgroundRes = R.drawable.widget_background,
        )
        val appearance = WidgetWallpaperStore.loadAppearance(key)
        WidgetTextSizes.applyCourseReminder(views, appearance.textScale)
        if (WidgetWallpaperRender.hasWallpaper(key)) {
            WidgetTextSizes.applyTextColors(views, context, dark, courseReminder = true)
        }
        views.setTextViewText(R.id.widget_title, titleText)
        views.setTextViewText(R.id.widget_week, weekText)

        if (displayCourses.isEmpty()) {
            views.setViewVisibility(R.id.widget_course1, View.GONE)
            views.setViewVisibility(R.id.widget_course2, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            views.setTextViewText(R.id.widget_empty_text, emptyText)
        } else {
            views.setViewVisibility(R.id.widget_empty, View.GONE)

            val c1 = displayCourses[0]
            views.setViewVisibility(R.id.widget_course1, View.VISIBLE)
            views.setTextViewText(R.id.widget_name1, c1.name)
            views.setTextViewText(R.id.widget_time_start1, c1.start)
            views.setTextViewText(R.id.widget_time_end1, c1.end)
            val remaining1 = if (showTomorrow) null else remainingMinutes(c1, currentMinutes)
            // 色条位图只有胶囊本身，四角透明；填底色会在半透明卡片上露出方角
            views.setBitmap(R.id.widget_color1, "setImageBitmap",
                createColorBarBitmap(context, c1.color))
            views.setViewVisibility(R.id.widget_color1, View.VISIBLE)
            views.setViewVisibility(R.id.widget_now1, if (remaining1 != null) View.VISIBLE else View.GONE)
            if (remaining1 != null) {
                views.setTextViewText(R.id.widget_now1, WidgetStrings.endsInN(remaining1))
            }
            WidgetWallpaperRender.applyCardBackground(
                views, R.id.widget_course1, key, dark, active = remaining1 != null,
            )
            views.setTextViewText(R.id.widget_info1, buildCourseInfo(c1, true))

            if (displayCourses.size >= 2) {
                val c2 = displayCourses[1]
                views.setViewVisibility(R.id.widget_course2, View.VISIBLE)
                views.setViewVisibility(R.id.widget_color2, View.VISIBLE)
                views.setTextViewText(R.id.widget_name2, c2.name)
                views.setTextViewText(R.id.widget_time_start2, c2.start)
                views.setTextViewText(R.id.widget_time_end2, c2.end)
                val remaining2 = if (showTomorrow) null else remainingMinutes(c2, currentMinutes)
                views.setBitmap(R.id.widget_color2, "setImageBitmap",
                    createColorBarBitmap(context, c2.color))
                views.setViewVisibility(R.id.widget_now2, if (remaining2 != null) View.VISIBLE else View.GONE)
                if (remaining2 != null) {
                    views.setTextViewText(R.id.widget_now2, WidgetStrings.endsInN(remaining2))
                }
                WidgetWallpaperRender.applyCardBackground(
                    views, R.id.widget_course2, key, dark, active = remaining2 != null,
                )
                views.setTextViewText(R.id.widget_info2, buildCourseInfo(c2, true))
            } else {
                // 无课时直接隐藏色条，不再塞占位位图，避免灰色竖杆/矩形残留
                views.setViewVisibility(R.id.widget_course2, View.GONE)
                views.setViewVisibility(R.id.widget_color2, View.GONE)
                views.setTextViewText(R.id.widget_name2, "")
                views.setTextViewText(R.id.widget_time_start2, "")
                views.setTextViewText(R.id.widget_time_end2, "")
                views.setTextViewText(R.id.widget_info2, "")
                views.setViewVisibility(R.id.widget_now2, View.GONE)
            }
        }

        // 任意位置点击 = 打开应用 + 刷新全部小部件
        views.setOnClickPendingIntent(
            R.id.widget_standard_root,
            WidgetClickHelper.launchAndRefreshPending(context)
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetUpdateCache.invalidateWidget("course_pad_$it") }
    }

    /**
     * 色条位图：整张图就是一条胶囊，四角留透明。
     * 填底色会在半透明卡片上露出方角。
     */
    private fun createColorBarBitmap(context: Context, color: Int): Bitmap {
        val density = WidgetTextSizes.deviceDensity(context)
        val width = (4 * density).toInt().coerceAtLeast(1)
        val height = (28 * density).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        // 端头半径取半宽 = 正好一个胶囊
        val radius = width / 2f
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
        return bitmap
    }
}
