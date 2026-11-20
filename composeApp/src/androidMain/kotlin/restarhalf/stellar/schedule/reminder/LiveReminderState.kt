package restarhalf.stellar.schedule.reminder

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

/**
 * 实况通知与超级岛的状态快照，照 Nexio 的 `countdown_state` / `island_countdown_state`。
 *
 * 进程被杀后刷新链仍要能对账，所以状态必须落盘，不能只放内存。
 * 课程 / 考试 / 测试课（[ReminderSession]）各用自己的键位，互不覆盖。
 */
internal object LiveReminderState : KoinComponent {

    private fun prefs(): ObservableSettings = get(named("reminder_codes"))

    /** 会话独立键位：课程沿用旧键（升级无缝），考试加 exam_、测试课加 test_。 */
    private fun key(base: String, session: ReminderSession): String =
        session.keyPrefix + base

    // ————————————————————————— 实况通知 —————————————————————————

    private const val K_ACTIVE = "live_active"
    private const val K_COURSE_NAME = "live_course_name"
    private const val K_CLASSROOM = "live_classroom"
    private const val K_SECTION = "live_section"
    private const val K_START_TIME = "live_start_time"
    private const val K_START_MILLIS = "live_start_millis"
    private const val K_END_MILLIS = "live_end_millis"
    private const val K_IN_CLASS_ACTIVE = "live_in_class_active"
    private const val K_LAST_DISPLAYED_MINUTES = "live_last_displayed_minutes"
    private const val K_LAST_IN_CLASS_MINUTES = "live_last_in_class_minutes"
    private const val K_LAST_IN_CLASS_PROGRESS = "live_last_in_class_progress"

    data class Countdown(
        val courseName: String,
        val classroom: String,
        val section: String,
        val startTime: String,
        val startMillis: Long,
        val endMillis: Long,
        val inClassActive: Boolean,
        val lastDisplayedMinutes: Int,
        val lastInClassMinutes: Int,
        val lastInClassProgress: Int,
    )

    fun isCountdownActive(session: ReminderSession = ReminderSession.Course): Boolean =
        prefs().getBoolean(key(K_ACTIVE, session), false)

    fun countdown(session: ReminderSession = ReminderSession.Course): Countdown? {
        val p = prefs()
        if (!p.getBoolean(key(K_ACTIVE, session), false)) return null
        val startMillis = p.getLong(key(K_START_MILLIS, session), 0L)
        if (startMillis <= 0L) return null
        return Countdown(
            courseName = p.getString(key(K_COURSE_NAME, session), ""),
            classroom = p.getString(key(K_CLASSROOM, session), ""),
            section = p.getString(key(K_SECTION, session), ""),
            startTime = p.getString(key(K_START_TIME, session), ""),
            startMillis = startMillis,
            endMillis = p.getLong(key(K_END_MILLIS, session), 0L),
            inClassActive = p.getBoolean(key(K_IN_CLASS_ACTIVE, session), false),
            lastDisplayedMinutes = p.getInt(key(K_LAST_DISPLAYED_MINUTES, session), -1),
            lastInClassMinutes = p.getInt(key(K_LAST_IN_CLASS_MINUTES, session), -1),
            lastInClassProgress = p.getInt(key(K_LAST_IN_CLASS_PROGRESS, session), -1),
        )
    }

    fun saveCountdown(
        courseName: String,
        classroom: String,
        section: String,
        startTime: String,
        startMillis: Long,
        endMillis: Long,
        session: ReminderSession = ReminderSession.Course,
    ) {
        prefs().apply {
            this[key(K_ACTIVE, session)] = true
            this[key(K_COURSE_NAME, session)] = courseName
            this[key(K_CLASSROOM, session)] = classroom
            this[key(K_SECTION, session)] = section
            this[key(K_START_TIME, session)] = startTime
            this[key(K_START_MILLIS, session)] = startMillis
            this[key(K_END_MILLIS, session)] = endMillis
            this[key(K_IN_CLASS_ACTIVE, session)] = false
            remove(key(K_LAST_DISPLAYED_MINUTES, session))
            remove(key(K_LAST_IN_CLASS_MINUTES, session))
            remove(key(K_LAST_IN_CLASS_PROGRESS, session))
        }
    }

    fun setLastDisplayedMinutes(
        minutes: Int,
        session: ReminderSession = ReminderSession.Course,
    ) {
        prefs()[key(K_LAST_DISPLAYED_MINUTES, session)] = minutes
    }

    fun setInClass(
        active: Boolean,
        minutes: Int = -1,
        progress: Int = -1,
        session: ReminderSession = ReminderSession.Course,
    ) {
        prefs().apply {
            this[key(K_IN_CLASS_ACTIVE, session)] = active
            if (minutes >= 0) this[key(K_LAST_IN_CLASS_MINUTES, session)] = minutes
            if (progress >= 0) this[key(K_LAST_IN_CLASS_PROGRESS, session)] = progress
        }
    }

    fun clearCountdown(session: ReminderSession = ReminderSession.Course) {
        prefs().apply {
            this[key(K_ACTIVE, session)] = false
            this[key(K_IN_CLASS_ACTIVE, session)] = false
            remove(key(K_LAST_DISPLAYED_MINUTES, session))
            remove(key(K_LAST_IN_CLASS_MINUTES, session))
            remove(key(K_LAST_IN_CLASS_PROGRESS, session))
        }
    }

    // ————————————————————————— 超级岛 —————————————————————————

    private const val K_ISLAND_ACTIVE = "island_active"
    private const val K_ISLAND_SWITCHED = "island_switched"
    private const val K_ISLAND_SWITCHED_AT = "island_switched_at"
    private const val K_ISLAND_NOTIFICATION_ID = "island_notification_id"
    private const val K_ISLAND_COURSE_NAME = "island_course_name"
    private const val K_ISLAND_CLASSROOM = "island_classroom"
    private const val K_ISLAND_SECTION = "island_section"
    private const val K_ISLAND_START_TIME = "island_start_time"
    private const val K_ISLAND_END_TIME = "island_end_time"
    private const val K_ISLAND_START_MILLIS = "island_start_millis"
    private const val K_ISLAND_END_MILLIS = "island_end_millis"

    data class Island(
        val notificationId: Int,
        val courseName: String,
        val classroom: String,
        val section: String,
        val startTime: String,
        val endTime: String,
        val startMillis: Long,
        val endMillis: Long,
        val switched: Boolean,
        val switchedAt: Long,
    )

    fun islandSnapshot(session: ReminderSession = ReminderSession.Course): Island? {
        val p = prefs()
        if (!p.getBoolean(key(K_ISLAND_ACTIVE, session), false)) return null
        val startMillis = p.getLong(key(K_ISLAND_START_MILLIS, session), 0L)
        if (startMillis <= 0L) return null
        return Island(
            notificationId =
                p.getInt(key(K_ISLAND_NOTIFICATION_ID, session), session.islandCountdownId),
            courseName = p.getString(key(K_ISLAND_COURSE_NAME, session), ""),
            classroom = p.getString(key(K_ISLAND_CLASSROOM, session), ""),
            section = p.getString(key(K_ISLAND_SECTION, session), ""),
            startTime = p.getString(key(K_ISLAND_START_TIME, session), ""),
            endTime = p.getString(key(K_ISLAND_END_TIME, session), ""),
            startMillis = startMillis,
            endMillis = p.getLong(key(K_ISLAND_END_MILLIS, session), 0L),
            switched = p.getBoolean(key(K_ISLAND_SWITCHED, session), false),
            switchedAt = p.getLong(key(K_ISLAND_SWITCHED_AT, session), 0L),
        )
    }

    fun saveIsland(
        notificationId: Int,
        courseName: String,
        classroom: String,
        section: String,
        startTime: String,
        endTime: String,
        startMillis: Long,
        endMillis: Long,
        session: ReminderSession = ReminderSession.Course,
    ) {
        prefs().apply {
            this[key(K_ISLAND_ACTIVE, session)] = true
            this[key(K_ISLAND_SWITCHED, session)] = false
            remove(key(K_ISLAND_SWITCHED_AT, session))
            this[key(K_ISLAND_NOTIFICATION_ID, session)] = notificationId
            this[key(K_ISLAND_COURSE_NAME, session)] = courseName
            this[key(K_ISLAND_CLASSROOM, session)] = classroom
            this[key(K_ISLAND_SECTION, session)] = section
            this[key(K_ISLAND_START_TIME, session)] = startTime
            this[key(K_ISLAND_END_TIME, session)] = endTime
            this[key(K_ISLAND_START_MILLIS, session)] = startMillis
            this[key(K_ISLAND_END_MILLIS, session)] = endMillis
        }
    }

    fun updateIslandNotificationId(
        notificationId: Int,
        session: ReminderSession = ReminderSession.Course,
    ) {
        prefs()[key(K_ISLAND_NOTIFICATION_ID, session)] = notificationId
    }

    fun markIslandSwitched(session: ReminderSession = ReminderSession.Course) {
        prefs().apply {
            this[key(K_ISLAND_SWITCHED, session)] = true
            this[key(K_ISLAND_SWITCHED_AT, session)] = System.currentTimeMillis()
        }
    }

    fun clearIsland(session: ReminderSession = ReminderSession.Course) {
        prefs()[key(K_ISLAND_ACTIVE, session)] = false
    }

    // ————————————————————————— 测试课窗口 —————————————————————————

    /**
     * 测试课（「测试小米超级岛」/「测试实时活动」）的课堂时间窗。
     * 与 Nexio 一致：不单独存一份窗口，直接从测试状态派生 ——
     * 实时活动看测试倒计时，超级岛看测试岛快照。
     * 这样收起实况通知、岛对账等无关清理不会误删勿扰要用的窗口。
     */
    fun testClassWindow(): Pair<Long, Long>? {
        val candidates =
            listOfNotNull(
                countdown(ReminderSession.CourseTest)
                    ?.takeIf { it.startMillis > 0L && it.endMillis > it.startMillis }
                    ?.let { it.startMillis to it.endMillis },
                islandSnapshot(ReminderSession.CourseTest)
                    ?.takeIf { it.startMillis > 0L && it.endMillis > it.startMillis }
                    ?.let { it.startMillis to it.endMillis },
            )
        if (candidates.isEmpty()) return null
        val now = System.currentTimeMillis()
        // 两轮测试重叠时取正在上课的那一轮，否则取最近的一轮
        return candidates.firstOrNull { now >= it.first && now < it.second }
            ?: candidates.maxByOrNull { it.first }
    }
}