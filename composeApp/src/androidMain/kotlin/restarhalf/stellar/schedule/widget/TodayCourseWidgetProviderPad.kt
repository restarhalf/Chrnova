/** 今日课程桌面小组件提供者 (Pad 1×1 版) */
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

class TodayCourseWidgetProviderPad : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "restarhalf.stellar.schedule.UPDATE_TODAY_WIDGET_PAD"

        private val dotIds = listOf(
            R.id.widget_dot1, R.id.widget_dot2, R.id.widget_dot3, R.id.widget_dot4,
            R.id.widget_dot5, R.id.widget_dot6, R.id.widget_dot7, R.id.widget_dot8
        )

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TodayCourseWidgetProviderPad::class.java).apply {
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
                logWidgetFailure("TodayCourseWidgetPad", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TodayCourseWidgetProviderPad::class.java)
            )
            if (appWidgetIds.isNotEmpty()) onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val key = WidgetWallpaperStore.KEY_SMALL
        val dark = WidgetWallpaperRender.resolveDark(context, key)
        val data = WidgetCourseResolver.load(dark)
        val currentMinutes = data.nowMinutes
        val showTomorrow = data.showTomorrow()

        val resolution = if (showTomorrow) data.tomorrow else data.today
        val titleText = if (showTomorrow) {
            WidgetStrings.TOMORROW
        } else {
            WidgetStrings.weekdayLabel(resolution.calendarDayOfWeek)
        }
        val weekText = weekTextOf(data, resolution)

        val displayCourse: WidgetCourse?
        val remainingCourses: List<WidgetCourse>
        val allCourses: List<WidgetCourse>
        val emptyText: String
        val startTime: String
        val endTime: String

        if (showTomorrow) {
            displayCourse = resolution.courses.firstOrNull()
            remainingCourses = resolution.courses.drop(1)
            allCourses = resolution.courses
        } else {
            val unfinishedCourses = data.today.courses.filter {
                WidgetTimeSpan.endOffsetMinutes(it.startMin, it.endMin) > currentMinutes
            }
            displayCourse = unfinishedCourses.firstOrNull()
            remainingCourses = unfinishedCourses.drop(1)
            allCourses = unfinishedCourses
        }

        if (displayCourse != null) {
            emptyText = ""
            startTime = displayCourse.start
            endTime = displayCourse.end
        } else {
            emptyText = emptyTextOf(data, resolution, showTomorrow)
            startTime = ""
            endTime = ""
        }

        // 上课瞬间圆点列表会从 allCourses 变为去掉当前课，签名必须含 is_now 与圆点摘要
        val inCourse = displayCourse != null &&
            WidgetTimeSpan.isActiveAt(displayCourse.startMin, displayCourse.endMin, currentMinutes)
        val dotCourses = if (displayCourse != null && !showTomorrow && inCourse) {
            allCourses.filter { it.id != displayCourse.id }
        } else if (displayCourse != null) {
            allCourses
        } else {
            emptyList()
        }

        val signature = buildString {
            append(dark).append('|').append(WidgetDeps.display.getWidgetPaddingMode())
            append('|').append(WidgetWallpaperRender.signaturePart(key))
            append('|').append(titleText).append('|').append(weekText)
            if (displayCourse != null) {
                append('|').append(displayCourse.id).append('|').append(displayCourse.name)
                append('|').append(startTime).append('-').append(endTime)
                append('|').append(displayCourse.location)
                append('|').append(remainingCourses.size)
                append('|').append(if (inCourse) "in" else "out")
                append('|').append(dotCourses.joinToString(",") { "${it.id}:${it.color}" })
            } else {
                append('|').append(emptyText)
            }
        }
        if (WidgetUpdateCache.shouldSkip("today_pad_$appWidgetId", signature)) return

        val views = RemoteViews(context.packageName, R.layout.widget_today_course_standard)
        WidgetWallpaperRender.apply(
            views = views,
            context = context,
            key = key,
            appWidgetId = appWidgetId,
            imageId = R.id.widget_wallpaper,
            containerId = R.id.widget_today_container,
            normalBackgroundRes = R.drawable.widget_today_course_background,
        )
        val appearance = WidgetWallpaperStore.loadAppearance(key)
        WidgetTextSizes.applyTodayCourse(views, appearance.textScale)
        if (WidgetWallpaperRender.hasWallpaper(key)) {
            WidgetTextSizes.applyTextColors(views, context, dark, courseReminder = false)
        }
        views.setTextViewText(R.id.widget_title, titleText)
        views.setTextViewText(R.id.widget_week, weekText)

        if (displayCourse != null) {
            views.setViewVisibility(R.id.widget_course_content, View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty, View.GONE)

            views.setTextViewText(R.id.widget_course_name, displayCourse.name)
            views.setTextViewText(R.id.widget_course_time, "$startTime-$endTime")
            views.setTextViewText(R.id.widget_course_location, displayCourse.location)

            val remainingCount = remainingCourses.size
            val remainingText = if (remainingCount > 0) {
                WidgetStrings.moreClassesN(remainingCount)
            } else {
                WidgetStrings.NO_OTHER_CLASSES
            }
            views.setTextViewText(R.id.widget_remaining_text, remainingText)

            for (i in dotIds.indices) {
                if (i < dotCourses.size) {
                    views.setViewVisibility(dotIds[i], View.VISIBLE)
                    views.setImageViewBitmap(
                        dotIds[i],
                        createCircleBitmap(context, dotCourses[i].color)
                    )
                } else {
                    views.setViewVisibility(dotIds[i], View.GONE)
                }
            }
        } else {
            views.setViewVisibility(R.id.widget_course_content, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            views.setTextViewText(R.id.widget_empty_text, emptyText)
            for (dotId in dotIds) {
                views.setViewVisibility(dotId, View.GONE)
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
        appWidgetIds.forEach { WidgetUpdateCache.invalidateWidget("today_pad_$it") }
    }

    /** 色球位图：整张图就是一个圆，四角留透明（填底色会在半透明卡片上露出方角）。 */
    private fun createCircleBitmap(context: Context, color: Int): Bitmap {
        val size = (7 * WidgetTextSizes.deviceDensity(context)).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return bitmap
    }
}
