package restarhalf.stellar.schedule.widget

import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.policy.effectiveCoursesForWeek
import restarhalf.stellar.schedule.domain.policy.isCourseActiveInWeek
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.ScheduleSnapshotPort
import restarhalf.stellar.schedule.ui.theme.pickCourseTitleColor
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal object WidgetDeps : KoinComponent {
    val snapshot: ScheduleSnapshotPort by inject()
    val display: DisplaySettingsPort by inject()
    val reminder: ReminderSettingsPort by inject()
}

/** 桌面小组件的一段课程（已换算成真正的起止时刻与分钟数）。 */
internal data class WidgetCourse(
    val id: Long,
    val name: String,
    val location: String,
    val teacher: String,
    val start: String,
    val end: String,
    val startMin: Int,
    val endMin: Int,
    val sectionStart: Int,
    val sectionEnd: Int,
    val color: Int,
)

/** 某一天的课程解析结果；[calendarDayOfWeek] 是日历星期，不受调休影响。 */
internal data class WidgetDay(
    val calendarDayOfWeek: Int,
    val week: Int,
    val isHolidayDate: Boolean,
    val courses: List<WidgetCourse>,
)

internal data class WidgetSnapshotData(
    val currentWeek: Int,
    val totalWeeks: Int,
    val lastWeekWithCourses: Int,
    val isHoliday: Boolean,
    val nowMinutes: Int,
    val today: WidgetDay,
    val tomorrow: WidgetDay,
    val nextDayReminderEnabled: Boolean,
    val nextDayReminderMinuteOfDay: Int,
)

/**
 * 小组件的数据入口：课程/校历走 [ScheduleSnapshotPort]，提醒与显示设置走各自 Port。
 *
 * 今日/明日统一按「日历日 + 教学周」解析：先定位到那一天的日期，再查该周生效的课程，
 * 按星期几过滤。假期（周数越界）时两天都返回空课表，由空态文案兜底。
 */
internal object WidgetCourseResolver {

    @OptIn(ExperimentalTime::class)
    suspend fun load(dark: Boolean): WidgetSnapshotData = withContext(Dispatchers.IO) {
        val snapshot = WidgetDeps.snapshot.load()
        val timetable = snapshot.timetable
        val totalWeeks = snapshot.totalWeeks
        val termStartMs = snapshot.termStartMs
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val nowMinutes = now.hour * 60 + now.minute

        val liveWeek = WeekCalculator.detect(totalWeeks = totalWeeks, termStartMs = termStartMs).week
        val lastWeekWithCourses = snapshot.courses.maxOfOrNull { course ->
            if (course.type == 2) course.targetWeek else course.weeks.maxOrNull() ?: 0
        } ?: 0
        val isHoliday = liveWeek > totalWeeks || (liveWeek >= 1 && liveWeek > lastWeekWithCourses)

        val nextDayEnabled = WidgetDeps.reminder.observeNextDayReminderEnabled().first()
        val nextDayMinute = WidgetDeps.reminder.observeNextDayReminderMinuteOfDay().first()

        WidgetSnapshotData(
            currentWeek = liveWeek,
            totalWeeks = totalWeeks,
            lastWeekWithCourses = lastWeekWithCourses,
            isHoliday = isHoliday,
            nowMinutes = nowMinutes,
            today = loadDay(snapshot.courses, timetable, totalWeeks, termStartMs, now, 0, dark),
            tomorrow = loadDay(snapshot.courses, timetable, totalWeeks, termStartMs, now, 1, dark),
            nextDayReminderEnabled = nextDayEnabled,
            nextDayReminderMinuteOfDay = nextDayMinute,
        )
    }

    private fun loadDay(
        courses: List<Course>,
        timetable: List<TimetableSlot>,
        totalWeeks: Int,
        termStartMs: Long,
        now: LocalDateTime,
        dayOffset: Int,
        dark: Boolean,
    ): WidgetDay {
        val dayDate = now.date.plus(dayOffset, DateTimeUnit.DAY)
        val dayCalMs = dayDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val weekInfo = WeekCalculator.detect(
            totalWeeks = totalWeeks,
            termStartMs = termStartMs,
            nowMs = dayCalMs + 12 * 60 * 60 * 1000L,
        )
        val weekday = dayDate.dayOfWeek.isoDayNumber
        val week = weekInfo.week

        val sessions = if (weekInfo.isHoliday) {
            emptyList()
        } else {
            effectiveCoursesForWeek(all = courses, week = week)
                .asSequence()
                .filter { it.dayOfWeek == weekday && isCourseActiveInWeek(it, week) }
                .mapNotNull { course -> toWidgetCourse(course, timetable, dark) }
                .sortedBy { it.startMin }
                .toList()
        }

        return WidgetDay(
            calendarDayOfWeek = weekday,
            week = week,
            isHolidayDate = weekInfo.isHoliday,
            courses = sessions,
        )
    }

    private fun toWidgetCourse(
        course: Course,
        timetable: List<TimetableSlot>,
        dark: Boolean,
    ): WidgetCourse? {
        val startSection = course.startSection
        val endSection = course.startSection + course.sectionCount - 1
        val start = timetable.getOrNull(startSection - 1)?.start ?: return null
        val end = timetable.getOrNull(endSection - 1)?.end ?: return null
        val startMin = ClockTime.parseToMinutes(start) ?: return null
        val endMin = ClockTime.parseToMinutes(end) ?: return null
        return WidgetCourse(
            id = course.id,
            name = course.name,
            location = course.location,
            teacher = course.teacher,
            start = start,
            end = end,
            startMin = startMin,
            endMin = endMin,
            sectionStart = startSection,
            sectionEnd = endSection,
            color = resolveBarColor(course, dark),
        )
    }

    private fun resolveBarColor(course: Course, dark: Boolean): Int {
        if (course.color.isNotBlank() && !course.color.equals(Course.SYNC_PLACEHOLDER_COLOR, true)) {
            runCatching { course.color.toColorInt() }.getOrNull()?.let { return it }
        }
        return pickCourseTitleColor(course.name, dark).toArgb()
    }
}

/** 时间段的跨午夜语义（与 Nexio 的 CourseTimeSpan 同一套）。 */
internal object WidgetTimeSpan {

    /** 时段长度（分钟），跨午夜时给 `endMin + 1440 - startMin`。 */
    fun endOffsetMinutes(startMin: Int, endMin: Int): Int =
        if (endMin > startMin) endMin else endMin + 1440

    /**
     * 「正在上课」判定。
     * 跨午夜只认 `[start, 24:00)` —— 0 点后的尾巴属于前一天；
     * 起止相同按非进行中处理（脏数据/解析失败）。
     */
    fun isActiveAt(startMin: Int, endMin: Int, currentMin: Int): Boolean = when {
        endMin < startMin -> currentMin >= startMin
        startMin == endMin -> false
        else -> currentMin in startMin until endMin
    }
}

/** 桌面小组件的文案（与 Nexio 的 AndroidWidgetStr 同口径）。 */
internal object WidgetStrings {
    const val TODAY = "今天"
    const val TOMORROW = "明天"
    const val TOMORROW_COURSES = "明日课程"
    const val ON_BREAK = "放假中"
    const val NOT_STARTED = "未开始"
    const val VACATION_NO_CLASSES = "假期中，暂无课程"
    const val SEMESTER_NOT_STARTED = "学期暂未开始"
    const val NO_CLASSES_TOMORROW = "明日无课"
    const val NO_CLASSES_TODAY = "今日无课"
    const val ALL_CLASSES_FINISHED = "今日课程已上完"
    const val NO_OTHER_CLASSES = "没有其他课程"
    const val INFO_SEPARATOR = "｜"

    fun endsInN(minutes: Int): String = "${minutes}分钟结束"
    fun moreClassesN(count: Int): String = "还有${count}节课"
    fun weekN(week: Int): String = "第${week}周"
    fun weekdayLabel(mon1: Int): String = when (mon1) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> "周一"
    }
}

/**
 * 小组件「今天显示今日还是明日」的判定：开启次日提醒、到点、且今日课程都已结束。
 * 与 Nexio 的 showTomorrow 同一套条件。
 */
internal fun WidgetSnapshotData.showTomorrow(): Boolean {
    if (!nextDayReminderEnabled) return false
    if (nowMinutes < nextDayReminderMinuteOfDay) return false
    val lastEnd = today.courses.maxOfOrNull { WidgetTimeSpan.endOffsetMinutes(it.startMin, it.endMin) }
        ?: return true
    return nowMinutes >= lastEnd
}

/** 当前课/下一课的倒计时（正在上课时返回距下课分钟数，否则 null）。 */
internal fun remainingMinutes(course: WidgetCourse, nowMinutes: Int): Int? =
    if (WidgetTimeSpan.isActiveAt(course.startMin, course.endMin, nowMinutes)) {
        WidgetTimeSpan.endOffsetMinutes(course.startMin, course.endMin) - nowMinutes
    } else {
        null
    }

/** 课程信息行：节次 + 教室 + 教师，非空项用「｜」分隔。 */
internal fun buildCourseInfo(course: WidgetCourse, includeSections: Boolean): String = buildList {
    if (includeSections && course.sectionStart > 0 && course.sectionEnd >= course.sectionStart) {
        add("第${course.sectionStart}-${course.sectionEnd}节")
    }
    if (course.location.isNotEmpty()) add(course.location)
    if (course.teacher.isNotEmpty()) add(course.teacher)
}.joinToString(WidgetStrings.INFO_SEPARATOR)

/** 空态文案：与 Nexio 的 when 链同序。 */
internal fun emptyTextOf(
    data: WidgetSnapshotData,
    resolution: WidgetDay,
    showTomorrow: Boolean,
): String = when {
    data.isHoliday -> WidgetStrings.VACATION_NO_CLASSES
    data.currentWeek < 1 -> WidgetStrings.SEMESTER_NOT_STARTED
    resolution.isHolidayDate -> WidgetStrings.VACATION_NO_CLASSES
    showTomorrow -> WidgetStrings.NO_CLASSES_TOMORROW
    data.today.courses.isEmpty() -> WidgetStrings.NO_CLASSES_TODAY
    else -> WidgetStrings.ALL_CLASSES_FINISHED
}

/** 表头右侧的教学周文案。 */
internal fun weekTextOf(data: WidgetSnapshotData, resolution: WidgetDay): String = when {
    data.isHoliday -> WidgetStrings.ON_BREAK
    data.currentWeek < 1 -> WidgetStrings.NOT_STARTED
    else -> WidgetStrings.weekN(resolution.week)
}

/** 桌面未放置任何小组件时跳过重绘前的日志兜底（便于排查远端异常）。 */
internal fun logWidgetFailure(tag: String, e: Exception) {
    if (e is kotlinx.coroutines.CancellationException) throw e
    Log.w(tag, "widget update failed", e)
}
