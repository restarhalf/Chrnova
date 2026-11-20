/** 今日课程桌面小组件提供者 (2×2 标准版：下一节课) */
package restarhalf.stellar.schedule.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.data.wallpaper.WidgetWallpaperStore

class TodayCourseWidgetProviderStandard : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "restarhalf.stellar.schedule.UPDATE_TODAY_WIDGET_STANDARD"

        /** ColorOS 会把 2×2 小部件内容整体放大的反补偿倍率 */
        private const val COLOR_OS_CONTENT_SCALE = 0.92f

        private val dotIds = listOf(
            R.id.widget_dot1, R.id.widget_dot2, R.id.widget_dot3, R.id.widget_dot4,
            R.id.widget_dot5, R.id.widget_dot6, R.id.widget_dot7, R.id.widget_dot8
        )

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TodayCourseWidgetProviderStandard::class.java).apply {
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
                logWidgetFailure("TodayCourseWidget", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TodayCourseWidgetProviderStandard::class.java)
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
            // 今日也用日历日
            WidgetStrings.weekdayLabel(resolution.calendarDayOfWeek)
        }
        val weekText = weekTextOf(data, resolution)

        val displayCourse: WidgetCourse?
        val remainingCourses: List<WidgetCourse>
        val allCourses: List<WidgetCourse>

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

        val emptyText: String
        val startTime: String
        val endTime: String
        val remainingCount: Int
        val dotCourses: List<WidgetCourse>

        if (displayCourse != null) {
            emptyText = ""
            startTime = displayCourse.start
            endTime = displayCourse.end
            remainingCount = remainingCourses.size
            val inCourse = WidgetTimeSpan.isActiveAt(displayCourse.startMin, displayCourse.endMin, currentMinutes)
            dotCourses = if (!showTomorrow && inCourse) {
                allCourses.filter { it.id != displayCourse.id }
            } else {
                allCourses
            }
        } else {
            emptyText = emptyTextOf(data, resolution, showTomorrow)
            startTime = ""
            endTime = ""
            remainingCount = -1
            dotCourses = emptyList()
        }

        val paddingMode = WidgetDeps.display.getWidgetPaddingMode()
        val inCourse = displayCourse != null && run {
            WidgetTimeSpan.isActiveAt(displayCourse.startMin, displayCourse.endMin, currentMinutes)
        }
        val signature = buildString {
            append(dark).append('|').append(paddingMode)
            append('|').append(WidgetWallpaperRender.signaturePart(key))
            append('|').append(
                WidgetWallpaperRender.sizeSignature(
                    appWidgetManager, appWidgetId, WidgetWallpaperRender.WidgetSize(110, 110)
                )
            )
            append('|').append(titleText).append('|').append(weekText)
            if (displayCourse != null) {
                append('|').append(displayCourse.id).append('|').append(displayCourse.name)
                append('|').append(startTime).append('|').append(endTime)
                append('|').append(displayCourse.location)
                append('|').append(remainingCount).append('|').append(if (inCourse) "in" else "out")
                append('|').append(dotCourses.joinToString(",") { "${it.id}:${it.color}" })
            } else {
                append('|').append(emptyText)
            }
        }
        if (WidgetUpdateCache.shouldSkip("today_std_$appWidgetId", signature)) return

        fun buildViews(size: WidgetWallpaperRender.WidgetSize): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_today_course_standard)
            val (insetTopDp, insetBottomDp) = applyWidgetMode(views, context)
            // 外层圆角：HyperOS（小米系）按系统规范 24dp，其余 20dp
            val containerBackground = if (WidgetTextSizes.isXiaomi) {
                R.drawable.widget_today_course_background_xiaomi
            } else {
                R.drawable.widget_today_course_background
            }
            views.setInt(R.id.widget_today_container, "setBackgroundResource", containerBackground)
            WidgetWallpaperRender.apply(
                views = views,
                context = context,
                key = key,
                appWidgetId = appWidgetId,
                imageId = R.id.widget_wallpaper,
                containerId = R.id.widget_today_container,
                normalBackgroundRes = containerBackground,
                insetTopDp = insetTopDp,
                insetBottomDp = insetBottomDp,
                size = size,
            )
            // ColorOS 上 2×2 的内容会被整体放大，按 0.85 倍反补偿（字号与间距一起）
            val contentScale = if (WidgetTextSizes.isColorOs) COLOR_OS_CONTENT_SCALE else 1f
            val appearance = WidgetWallpaperStore.loadAppearance(key)
            WidgetTextSizes.applyTodayCourse(views, contentScale * appearance.textScale)
            if (WidgetWallpaperRender.hasWallpaper(key)) {
                WidgetTextSizes.applyTextColors(views, context, dark, courseReminder = false)
            }
            applyScaledMetrics(views, context, contentScale)
            views.setTextViewText(R.id.widget_title, titleText)
            views.setTextViewText(R.id.widget_week, weekText)

            if (displayCourse != null) {
                views.setViewVisibility(R.id.widget_course_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty, View.GONE)

                views.setTextViewText(R.id.widget_course_name, displayCourse.name)
                views.setTextViewText(R.id.widget_course_time, "$startTime-$endTime")
                views.setTextViewText(R.id.widget_course_location, displayCourse.location)

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
                            createCircleBitmap(context, dotCourses[i].color, contentScale)
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

            return views
        }
        val sizes = WidgetWallpaperRender.renderSizes(
            manager = appWidgetManager,
            appWidgetId = appWidgetId,
            fallback = WidgetWallpaperRender.WidgetSize(110, 110),
        )
        appWidgetManager.updateAppWidget(
            appWidgetId,
            WidgetWallpaperRender.responsiveViews(sizes.map { it to buildViews(it) }),
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        // 拉伸/缩放：尺寸变了必须重建（签名里也带了尺寸，这里再兜一层）
        WidgetUpdateCache.invalidateWidget("today_std_$appWidgetId")
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetUpdateCache.invalidateWidget("today_std_$it") }
    }

    /** 根容器上下边距按留白档位内缩（与 4×2 版同一张表）。 */
    private fun applyWidgetMode(views: RemoteViews, context: Context): Pair<Float, Float> {
        val (top, bottom) = WidgetWallpaperRender.verticalPaddingDp(WidgetDeps.display.getWidgetPaddingMode())
        views.setViewPadding(
            R.id.widget_standard_root,
            0,
            WidgetTextSizes.dpToPx(context, top).toInt(),
            0,
            WidgetTextSizes.dpToPx(context, bottom).toInt()
        )
        return top to bottom
    }

    /**
     * 间距随倍率缩放（与字号一起），用于 ColorOS 的反补偿。
     * 每次都显式下发全部数值：回到 1× 时会覆盖回原始值，不残留上次的缩放。
     */
    private fun applyScaledMetrics(views: RemoteViews, context: Context, scale: Float) {
        val unit = TypedValue.COMPLEX_UNIT_DIP
        // setViewPadding 的带单位重载是 API 37 才有的，这里统一用 px 版（API 1 起即可用）
        fun px(value: Float): Int = WidgetTextSizes.dpToPx(context, value * scale).toInt()
        fun margin(id: Int, type: Int, dp: Float) {
            views.setLayoutMarginCompat(id, type, dp * scale, unit)
        }

        views.setViewPadding(R.id.widget_today_container, px(10f), px(12f), px(10f), px(12f))
        margin(R.id.widget_header, RemoteViews.MARGIN_START, 4f)
        margin(R.id.widget_header, RemoteViews.MARGIN_END, 4f)
        margin(R.id.widget_header, RemoteViews.MARGIN_BOTTOM, 8f)
        views.setViewPadding(R.id.widget_course_name, px(4f), 0, 0, px(4f))
        margin(R.id.widget_course_name, RemoteViews.MARGIN_BOTTOM, 4f)
        dotIds.forEach { dot ->
            views.setLayoutWidthCompat(dot, 7f * scale, unit)
            views.setLayoutHeightCompat(dot, 7f * scale, unit)
        }
    }

    /**
     * 色球位图：整张图就是一个圆，四角留透明。
     * 填底色会在半透明卡片上露出方角。
     */
    private fun createCircleBitmap(context: Context, color: Int, scale: Float = 1f): Bitmap {
        val size = (7 * scale * WidgetTextSizes.deviceDensity(context)).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return bitmap
    }
}
