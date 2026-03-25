package restarhalf.stellar.schedule.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

object AcademicCalendar {
    @OptIn(ExperimentalTime::class)
    fun getTermStartMonday(termStartMs: Long): LocalDate {
        val termStart = kotlin.time.Instant.fromEpochMilliseconds(termStartMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dayOfWeek = termStart.dayOfWeek
        return termStart.minus(dayOfWeek.ordinal, kotlinx.datetime.DateTimeUnit.DAY)
    }

    fun getWeekDates(
        week: Int,
        termStartMs: Long,
    ): List<String> {
        val weekStart =
            getTermStartMonday(termStartMs).plus((week - 1) * 7, kotlinx.datetime.DateTimeUnit.DAY)

        val dateFormat = LocalDate.Format {
            monthNumber()
            char('/')
            this@Format.day(padding = Padding.ZERO)
        }

        return (0..6).map { dayOffset ->
            val date = weekStart.plus(dayOffset, kotlinx.datetime.DateTimeUnit.DAY)
            date.format(dateFormat)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getTodayIndexInWeek(
        week: Int,
        termStartMs: Long,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    ): Int? {
        if (week <= 0) return null
        val weekStart =
            getTermStartMonday(termStartMs).plus((week - 1) * 7, kotlinx.datetime.DateTimeUnit.DAY)
        val today = kotlin.time.Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        for (dayIndex in 0..6) {
            val dayDate = weekStart.plus(dayIndex, kotlinx.datetime.DateTimeUnit.DAY)
            if (dayDate == today) {
                return dayIndex
            }
        }
        return null
    }

    @OptIn(ExperimentalTime::class)
    fun getCurrentWeekMonday(
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    ): LocalDate {
        val today = kotlin.time.Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.minus(today.dayOfWeek.ordinal, kotlinx.datetime.DateTimeUnit.DAY)
    }

    @OptIn(ExperimentalTime::class)
    fun getCurrentWeekDates(
        dayCount: Int = 7,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    ): List<String> {
        val weekStart = getCurrentWeekMonday(nowMs)
        val dateFormat = LocalDate.Format {
            monthNumber()
            char('/')
            this@Format.day(padding = Padding.ZERO)
        }
        return (0..6)
            .map { dayOffset ->
                val date = weekStart.plus(dayOffset, kotlinx.datetime.DateTimeUnit.DAY)
                date.format(dateFormat)
            }
            .take(dayCount)
    }

    @OptIn(ExperimentalTime::class)
    fun getTodayIndexInCurrentWeek(
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    ): Int {
        val today = kotlin.time.Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.dayOfWeek.ordinal
    }
}
