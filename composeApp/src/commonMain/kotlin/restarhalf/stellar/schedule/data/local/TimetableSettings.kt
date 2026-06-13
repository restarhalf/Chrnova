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
import kotlin.time.ExperimentalTime

/**
 * 课表设置管理类
 * 
 * 负责课表相关设置的读写操作，包括：
 * - 校区设置
 * - 学期开始时间
 * - 总周数
 * 
 * @param settings ObservableSettings实例
 */
class TimetableSettings(private val settings: ObservableSettings) {

    /**
     * 获取当前校区
     * 
     * @return 校区枚举
     */
    fun getCampus(): Campus {
        val raw = settings.getString(KEY_CAMPUS, Campus.Development.name)
        return runCatching { Campus.valueOf(raw) }.getOrElse { Campus.Development }
    }

    /**
     * 观察校区变化
     * 
     * @return 校区Flow
     */
    @OptIn(ExperimentalSettingsApi::class)
    fun observeCampus(): Flow<Campus> {
        return settings.getStringFlow(KEY_CAMPUS, Campus.Development.name)
            .map { raw -> runCatching { Campus.valueOf(raw) }.getOrElse { Campus.Development } }
    }

    /**
     * 设置校区
     * 
     * @param campus 校区枚举
     */
    fun setCampus(campus: Campus) {
        settings[KEY_CAMPUS] = campus.name
    }

    /**
     * 获取学期开始时间戳
     * 
     * @return 时间戳（毫秒）
     */
    fun getTermStartMs(): Long {
        return settings.getLong(KEY_TERM_START_MS, defaultTermStartMs())
    }

    /**
     * 观察学期开始时间变化
     * 
     * @return 时间戳Flow
     */
    @OptIn(ExperimentalSettingsApi::class)
    fun observeTermStartMs(): Flow<Long> {
        return settings.getLongFlow(KEY_TERM_START_MS, defaultTermStartMs())
    }

    /**
     * 设置学期开始时间戳
     * 
     * @param ms 时间戳（毫秒）
     */
    fun setTermStartMs(ms: Long) {
        settings[KEY_TERM_START_MS] = ms
    }

    /**
     * 获取学期总周数
     * 
     * @return 总周数
     */
    fun getTotalWeeks(): Int {
        return settings.getInt(KEY_TOTAL_WEEKS, DEFAULT_TOTAL_WEEKS)
    }

    /**
     * 观察总周数变化
     * 
     * @return 总周数Flow
     */
    @OptIn(ExperimentalSettingsApi::class)
    fun observeTotalWeeks(): Flow<Int> {
        return settings.getIntFlow(KEY_TOTAL_WEEKS, DEFAULT_TOTAL_WEEKS)
    }

    /**
     * 设置学期总周数
     * 
     * @param weeks 总周数
     */
    fun setTotalWeeks(weeks: Int) {
        settings[KEY_TOTAL_WEEKS] = weeks
    }

    companion object {
        private const val PREFS_NAME = "timetable_prefs"
        private const val KEY_CAMPUS = "campus"
        private const val KEY_TERM_START_MS = "term_start_ms"
        private const val KEY_TOTAL_WEEKS = "total_weeks"
        private const val DEFAULT_TOTAL_WEEKS = 20

        /**
         * 获取默认学期开始时间
         * 
         * @return 默认时间戳（2026年3月2日）
         */
        @OptIn(ExperimentalTime::class)
        fun defaultTermStartMs(): Long {
            val date = LocalDate(2026, 3, 2)
            return date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }
    }
}

/**
 * 校区枚举
 */
enum class Campus {
    /** 开发校区 */
    Development,
    /** 金石滩校区 */
    Jinshitan
}

/**
 * 课表时间槽
 * 
 * @param num 节次编号
 * @param start 开始时间
 * @param end 结束时间
 */
data class TimetableSlot(val num: Int, val start: String, val end: String)

/** 开发校区课表时间配置 */
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

/** 金石滩校区课表时间配置 */
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

/**
 * 获取校区课表时间配置
 * 
 * @param campus 校区
 * @return 时间槽列表
 */
fun getCampusTimetable(campus: Campus): List<TimetableSlot> =
    when (campus) {
        Campus.Development -> DEVELOPMENT_TIMETABLE
        Campus.Jinshitan -> JINSHITAN_TIMETABLE
    }
