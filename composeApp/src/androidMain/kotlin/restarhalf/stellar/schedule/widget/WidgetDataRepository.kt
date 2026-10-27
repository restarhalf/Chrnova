package restarhalf.stellar.schedule.widget

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.domain.policy.effectiveCoursesForWeek
import restarhalf.stellar.schedule.domain.policy.isCourseActiveInWeek
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.ScheduleSnapshotPort
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Immutable
internal data class SmallWidgetState(
    val dayLabel: String,
    val weekLabel: String,
    val nextCourse: SmallNextCourse?,
    val emptyText: String,
)

internal data class SmallNextCourse(
    val title: String,
    val start: String,
    val end: String,
    val location: String,
    val restCount: Int,
    val dotColors: List<Color>,
)

@Immutable
internal data class LargeWidgetState(
    val headerLabel: String,
    val weekLabel: String,
    val rows: ImmutableList<LargeCourseRow>,
    val emptyText: String?,
)

internal data class LargeCourseRow(
    val id: Long,
    val start: String,
    val end: String,
    val title: String,
    val subtitle: String,
    val barColor: Color,
    val statusText: String?,
    val statusType: StatusType,
)

internal enum class StatusType {
    NONE,
    UPCOMING,
    ONGOING,
}

internal data class WidgetSnapshot(val small: SmallWidgetState, val large: LargeWidgetState)

private data class DayCourses(
    val offset: Int,
    val week: Int,
    val weekday: Int,
    val isHoliday: Boolean,
    val sessions: List<Session>,
)

private data class FocusStatus(val type: StatusType, val text: String)

private data class Session(
    val id: Long,
    val start: String,
    val end: String,
    val startMin: Int,
    val endMin: Int,
    val startSection: Int,
    val endSection: Int,
    val title: String,
    val location: String,
    val teacher: String,
    val barColor: Color,
)

internal object WidgetDataRepository {

    private const val UPCOMING_WINDOW_MINUTES = 10

    private const val MAX_ROWS = 2

    @OptIn(ExperimentalTime::class)
    suspend fun load(snapshotPort: ScheduleSnapshotPort): WidgetSnapshot =
        withContext(Dispatchers.IO) {
            val snapshot = snapshotPort.load()
            val filteredCourses = snapshot.courses
            val timetable = snapshot.timetable
            val totalWeeks = snapshot.totalWeeks
            val termStartMs = snapshot.termStartMs

            val now =
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val nowMinutes = now.hour * 60 + now.minute

            val today = loadDayCourses(filteredCourses, timetable, totalWeeks, termStartMs, now, 0)
            val todayRemaining = today.sessions.filter { it.endMin > nowMinutes }

            if (todayRemaining.isNotEmpty()) {
                return@withContext WidgetSnapshot(
                    small = buildSmallFromToday(today, nowMinutes),
                    large = buildLargeFromToday(today, nowMinutes)
                )
            }

            val tomorrow =
                loadDayCourses(filteredCourses, timetable, totalWeeks, termStartMs, now, 1)
            if (tomorrow.sessions.isNotEmpty()) {
                return@withContext WidgetSnapshot(
                    small = buildSmallFromTomorrow(tomorrow),
                    large = buildLargeFromTomorrow(tomorrow)
                )
            }

            val weekday = today.weekday
            val remainingDaysCount = (7 - weekday).coerceAtLeast(0)
            val remainingDays =
                if (remainingDaysCount > 0) {
                    (2..remainingDaysCount).map { offset ->
                        loadDayCourses(
                            filteredCourses,
                            timetable,
                            totalWeeks,
                            termStartMs,
                            now,
                            offset
                        )
                    }
                } else {
                    emptyList()
                }

            val thisWeekLaterHasCourse = remainingDays.any { it.sessions.isNotEmpty() }

            WidgetSnapshot(
                small = buildSmallEmpty(today, thisWeekLaterHasCourse),
                large = buildLargeEmpty(today, thisWeekLaterHasCourse)
            )
        }

    @OptIn(ExperimentalTime::class)
    private fun loadDayCourses(
        courses: List<Course>,
        timetable: List<TimetableSlot>,
        totalWeeks: Int,
        termStartMs: Long,
        now: LocalDateTime,
        dayOffset: Int,
    ): DayCourses {
        val dayDate = now.date.plus(dayOffset, DateTimeUnit.DAY)
        val dayCalMs = dayDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

        val weekInfo =
            WeekCalculator.detect(
                totalWeeks = totalWeeks,
                termStartMs = termStartMs,
                nowMs = dayCalMs + (12 * 60 * 60 * 1000L)
            )
        val weekday = dayDate.dayOfWeek.isoDayNumber
        val week = weekInfo.week

        val effective =
            if (weekInfo.isHoliday) emptyList() else effectiveCoursesForWeek(
                all = courses,
                week = week
            )
        val sessions =
            if (weekInfo.isHoliday) {
                emptyList()
            } else {
                effective
                    .asSequence()
                    .filter { it.dayOfWeek == weekday && isCourseActiveInWeek(it, week) }
                    .mapNotNull { course -> toSession(course, timetable) }
                    .sortedBy { it.startMin }
                    .toList()
            }

        return DayCourses(
            offset = dayOffset,
            week = week,
            weekday = weekday,
            isHoliday = weekInfo.isHoliday,
            sessions = sessions
        )
    }

    private fun buildSmallFromToday(today: DayCourses, nowMinutes: Int): SmallWidgetState {
        val todayRemaining = today.sessions.filter { it.endMin > nowMinutes }
        val first = todayRemaining.first()
        return SmallWidgetState(
            dayLabel = weekdayLabel(today.weekday),
            weekLabel = weekLabelOf(today),
            nextCourse =
                SmallNextCourse(
                    title = first.title,
                    start = first.start,
                    end = first.end,
                    location = first.location,
                    restCount = (todayRemaining.size - 1).coerceAtLeast(0),
                    dotColors = todayRemaining.drop(1).map { it.barColor },
                ),
            emptyText = ""
        )
    }

    private fun buildSmallFromTomorrow(tomorrow: DayCourses): SmallWidgetState {
        val first = tomorrow.sessions.first()
        return SmallWidgetState(
            dayLabel = "明天",
            weekLabel = weekLabelOf(tomorrow),
            nextCourse =
                SmallNextCourse(
                    title = first.title,
                    start = first.start,
                    end = first.end,
                    location = first.location,
                    restCount = (tomorrow.sessions.size - 1).coerceAtLeast(0),
                    dotColors = tomorrow.sessions.drop(1).map { it.barColor },
                ),
            emptyText = ""
        )
    }

    private fun buildSmallEmpty(
        today: DayCourses,
        thisWeekLaterHasCourse: Boolean,
    ): SmallWidgetState {
        return SmallWidgetState(
            dayLabel = weekdayLabel(today.weekday),
            weekLabel = weekLabelOf(today),
            nextCourse = null,
            emptyText = emptyTextOf(today, thisWeekLaterHasCourse),
        )
    }

    private fun buildLargeFromToday(today: DayCourses, nowMinutes: Int): LargeWidgetState {
        val todayRemaining = today.sessions.filter { it.endMin > nowMinutes }
        val rows =
            todayRemaining.take(MAX_ROWS).map { session ->
                val status = pickFocusStatus(session, nowMinutes)
                session.toLargeRow(status?.type ?: StatusType.NONE, status?.text)
            }

        return LargeWidgetState(
            headerLabel = "今天 / ${weekdayLabel(today.weekday)}",
            weekLabel = weekLabelOf(today),
            rows = rows.toPersistentList(),
            emptyText = null,
        )
    }

    private fun buildLargeFromTomorrow(tomorrow: DayCourses): LargeWidgetState =
        LargeWidgetState(
            headerLabel = "明天课程预告 / ${weekdayLabel(tomorrow.weekday)}",
            weekLabel = weekLabelOf(tomorrow),
            rows = tomorrow.sessions.take(MAX_ROWS)
                .map { it.toLargeRow(StatusType.NONE, null) }
                .toPersistentList(),
            emptyText = null,
        )

    private fun buildLargeEmpty(
        today: DayCourses,
        thisWeekLaterHasCourse: Boolean,
    ): LargeWidgetState =
        LargeWidgetState(
            headerLabel = "今天 / ${weekdayLabel(today.weekday)}",
            weekLabel = weekLabelOf(today),
            rows = persistentListOf(),
            emptyText = emptyTextOf(today, thisWeekLaterHasCourse),
        )

    private fun emptyTextOf(today: DayCourses, thisWeekLaterHasCourse: Boolean): String =
        when {
            today.isHoliday -> "假期快乐~!"
            today.sessions.isEmpty() && !thisWeekLaterHasCourse -> "恭喜！本周课程已通关~"
            today.sessions.isNotEmpty() -> "今日课程已上完"
            else -> "今明两天无课"
        }

    private fun weekLabelOf(day: DayCourses): String =
        if (day.isHoliday) "假期中" else "第${day.week}周"

    private fun pickFocusStatus(session: Session, nowMinutes: Int): FocusStatus? {
        if (nowMinutes >= session.startMin && nowMinutes < session.endMin) {
            return FocusStatus(
                type = StatusType.ONGOING,
                text = "${(session.endMin - nowMinutes).coerceAtLeast(0)}分钟结束"
            )
        }

        val ahead = session.startMin - nowMinutes
        if (nowMinutes < session.startMin && ahead in 1..UPCOMING_WINDOW_MINUTES) {
            return FocusStatus(
                type = StatusType.UPCOMING,
                text = "${ahead.coerceAtLeast(0)}分钟后"
            )
        }
        return null
    }

    private fun toSession(
        course: Course,
        timetable: List<TimetableSlot>,
    ): Session? {
        val startSection = course.startSection
        val endSection = course.startSection + course.sectionCount - 1
        val start = timetable.getOrNull(startSection - 1)?.start ?: return null
        val end = timetable.getOrNull(endSection - 1)?.end ?: return null
        val startMin = ClockTime.parseToMinutes(start) ?: return null
        val endMin = ClockTime.parseToMinutes(end) ?: return null
        return Session(
            id = course.id,
            start = start,
            end = end,
            startMin = startMin,
            endMin = endMin,
            startSection = startSection,
            endSection = endSection,
            title = course.name,
            location = course.location,
            teacher = course.teacher,
            barColor = parseCourseColor(course.color) ?: Color(0xFF5F89FF)
        )
    }

    private fun Session.toLargeRow(statusType: StatusType, statusText: String?): LargeCourseRow {
        val parts = mutableListOf("第${startSection}-${endSection}节")
        if (location.isNotBlank()) parts.add(location)
        if (teacher.isNotBlank()) parts.add(teacher)
        return LargeCourseRow(
            id = id,
            start = start,
            end = end,
            title = title,
            subtitle = parts.joinToString(" | "),
            barColor = barColor,
            statusText = statusText,
            statusType = statusType
        )
    }

    private fun weekdayLabel(mon1: Int): String =
        when (mon1) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> "周一"
        }

    private fun parseCourseColor(raw: String): Color? {
        if (raw.isBlank()) return null
        return runCatching { Color(raw.toColorInt()) }
            .onFailure {
                AppLogger.log("Widget", "解析课程颜色失败: raw=$raw", it)
            }
            .getOrNull()
    }
}
