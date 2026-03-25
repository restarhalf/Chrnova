package restarhalf.stellar.schedule.data.local

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.ExperimentalTime

class TimetableSettings(private val settings: ObservableSettings) {

    fun getCampus(): Campus {
        val raw = settings.getString(KEY_CAMPUS, Campus.Development.name)
        return runCatching { Campus.valueOf(raw) }.getOrElse { Campus.Development }
    }

    fun setCampus(campus: Campus) {
        settings[KEY_CAMPUS] = campus.name
    }

    fun getTermStartMs(): Long {
        return settings.getLong(KEY_TERM_START_MS, defaultTermStartMs())
    }

    fun setTermStartMs(ms: Long) {
        settings[KEY_TERM_START_MS] = ms
    }

    fun getTotalWeeks(): Int {
        return settings.getInt(KEY_TOTAL_WEEKS, DEFAULT_TOTAL_WEEKS)
    }

    fun setTotalWeeks(weeks: Int) {
        settings[KEY_TOTAL_WEEKS] = weeks
    }

    companion object {
        private const val PREFS_NAME = "timetable_prefs"
        private const val KEY_CAMPUS = "campus"
        private const val KEY_TERM_START_MS = "term_start_ms"
        private const val KEY_TOTAL_WEEKS = "total_weeks"
        private const val DEFAULT_TOTAL_WEEKS = 20

        @OptIn(ExperimentalTime::class)
        fun defaultTermStartMs(): Long {
            val date = LocalDate(2026, 3, 2)
            return date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }
    }
}

enum class Campus {
    Development,
    Jinshitan
}

data class TimetableSlot(val num: Int, val start: String, val end: String)

private val DEVELOPMENT_TIMETABLE =
    listOf(
        TimetableSlot(1, "8:00", "8:45"),
        TimetableSlot(2, "8:55", "9:40"),
        TimetableSlot(3, "10:00", "10:45"),
        TimetableSlot(4, "10:55", "11:40"),
        TimetableSlot(5, "13:30", "14:15"),
        TimetableSlot(6, "14:25", "15:10"),
        TimetableSlot(7, "15:20", "16:05"),
        TimetableSlot(8, "16:15", "17:00"),
        TimetableSlot(9, "18:00", "18:45"),
        TimetableSlot(10, "18:55", "19:40"),
        TimetableSlot(11, "20:00", "20:45"),
        TimetableSlot(12, "20:55", "21:40")
    )

private val JINSHITAN_TIMETABLE =
    listOf(
        TimetableSlot(1, "8:30", "9:10"),
        TimetableSlot(2, "9:20", "10:00"),
        TimetableSlot(3, "10:20", "11:00"),
        TimetableSlot(4, "11:10", "11:50"),
        TimetableSlot(5, "13:30", "14:10"),
        TimetableSlot(6, "14:20", "15:00"),
        TimetableSlot(7, "15:20", "16:00"),
        TimetableSlot(8, "16:10", "16:50"),
        TimetableSlot(9, "18:30", "19:10"),
        TimetableSlot(10, "19:20", "20:00"),
        TimetableSlot(11, "20:10", "20:50"),
        TimetableSlot(12, "21:00", "21:40")
    )

fun getCampusTimetable(campus: Campus): List<TimetableSlot> =
    when (campus) {
        Campus.Development -> DEVELOPMENT_TIMETABLE
        Campus.Jinshitan -> JINSHITAN_TIMETABLE
    }
