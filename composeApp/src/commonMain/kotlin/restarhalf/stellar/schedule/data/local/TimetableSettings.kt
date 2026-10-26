package restarhalf.stellar.schedule.data.local

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getLongFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Campus
import kotlin.time.ExperimentalTime

class TimetableSettings(private val settings: ObservableSettings) {

    fun getCampus(): Campus {
        val raw = settings.getString(KEY_CAMPUS, Campus.Development.name)
        return runCatching { Campus.valueOf(raw) }
            .onFailure {
                AppLogger.log("Settings", "解析校区失败: raw=$raw", it)
            }
            .getOrElse { Campus.Development }
    }

    @OptIn(ExperimentalSettingsApi::class)
    fun observeCampus(): Flow<Campus> {
        return settings.getStringFlow(KEY_CAMPUS, Campus.Development.name)
            .map { raw ->
                runCatching { Campus.valueOf(raw) }
                    .onFailure {
                        AppLogger.log("Settings", "解析校区失败: raw=$raw", it)
                    }
                    .getOrElse { Campus.Development }
            }
    }

    fun setCampus(campus: Campus) {
        settings[KEY_CAMPUS] = campus.name
    }

    fun getTermStartMs(): Long {
        return settings.getLong(KEY_TERM_START_MS, defaultTermStartMs())
    }

    @OptIn(ExperimentalSettingsApi::class)
    fun observeTermStartMs(): Flow<Long> {
        return settings.getLongFlow(KEY_TERM_START_MS, defaultTermStartMs())
    }

    fun setTermStartMs(ms: Long) {
        settings[KEY_TERM_START_MS] = ms
    }

    fun getTotalWeeks(): Int {
        return settings.getInt(KEY_TOTAL_WEEKS, DEFAULT_TOTAL_WEEKS)
    }

    @OptIn(ExperimentalSettingsApi::class)
    fun observeTotalWeeks(): Flow<Int> {
        return settings.getIntFlow(KEY_TOTAL_WEEKS, DEFAULT_TOTAL_WEEKS)
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
