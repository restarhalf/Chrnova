package restarhalf.stellar.schedule.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.reminder.CourseReminderPlanner
import restarhalf.stellar.schedule.domain.reminder.CourseSlot
import restarhalf.stellar.schedule.domain.reminder.ReminderTiming
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.reminder.receiver.ClassDndReceiver
import restarhalf.stellar.schedule.reminder.receiver.CourseReminderReceiver
import restarhalf.stellar.schedule.reminder.receiver.NextDayReminderReceiver
import restarhalf.stellar.schedule.reminder.receiver.postCourseReminderNotification
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DaySchedule(
    val nextTriggerAtMs: Long?,
    val nextTriggerRc: Int?,
)

/**
 * 课程相关闹钟，排法照 Nexio 的 scheduleAllAlarms：
 *
 * - 不是「只排下一条」，而是把窗口内每一天的**每节课**都排上：
 *   课前提醒、上课瞬间（课中提醒）、上课/下课两个勿扰边界各一条，
 *   全部用课程 id 派生的 request code，并在登记表里记下，取消时按登记表清，
 *   课程被删也不会留下孤儿闹钟。
 * - 课前提醒遇上课间短于提前分钟数时，改在上一节下课时提醒，避免落进上一节课里。
 * - 触发点已过但还没上课的（闹钟丢失 / Doze 延迟 / 刚打开应用），立即补发，按课次去重。
 * - 边界闹钟走 setAlarmClock，Doze 下也能准点；次日提醒仍用 setExactAndAllowWhileIdle。
 */
internal object CourseDayAlarmScheduler : KoinComponent {

    private val courseRepository: CourseRepository by inject()
    private val timetable: TimetablePort by inject()

    const val RC_NEXT_DAY = 23001

    private const val RC_PRE_BASE = 100_000
    // 旧「课中提醒」闹钟段，保留作登记表分界与升级残留清理
    private const val RC_IN_CLASS_BASE = 200_000
    private const val RC_DND_START_BASE = 300_000
    private const val RC_DND_END_BASE = 400_000

    /** 测试课的勿扰起止，照 Nexio 的 39901/49901；不占用课程 id 段 */
    private const val RC_DND_TEST_START = 39901
    private const val RC_DND_TEST_END = 49901

    /** 旧版固定 request code，升级后清理残留闹钟 */
    private const val RC_LEGACY_PRE = 20001
    private const val RC_LEGACY_DND_START = 24001
    private const val RC_LEGACY_DND_END = 24002

    private const val KEY_REGISTRY = "course_alarm_rcs"

    // 发送去重照 Nexio：按课次 id 记时间戳的 JSON map，跨日整份失效。
    // 课前提醒 60 分钟内不重发。
    private const val KEY_PRE_SENT_DAY = "course_pre_sent_day"
    private const val KEY_PRE_SENT_MAP = "course_pre_sent_map"
    private const val PRE_SENT_DEDUP_WINDOW_MS = 60 * 60 * 1000L

    /** 每天可用的 request code 槽位数；两天同时排也不冲突 */
    private const val SLOTS_PER_DAY = 40_000

    const val EXTRA_COURSE_ID = "extra_course_id"
    const val EXTRA_START_AT = "extra_start_at"
    const val EXTRA_NAME = "extra_course_name"
    const val EXTRA_PLACE = "extra_course_place"
    const val EXTRA_START = "extra_course_start"
    const val EXTRA_END = "extra_course_end"

    /**
     * 排 [daySlots]（dayOffset 与当天课程）的全部闹钟。
     * 返回最早的一条课前提醒，供「下次提醒」摘要与 hasScheduledAlarm 使用。
     */
    fun scheduleDays(
        context: Context,
        bookkeeping: ObservableSettings,
        prefs: ObservableSettings,
        daySlots: List<Pair<Int, List<CourseSlot>>>,
        nowMs: Long,
    ): DaySchedule {
        cancelCourseAlarms(context, bookkeeping)

        val preEnabled = prefs.getBoolean(SettingsKeys.REMINDER_ENABLED, false)
        val dndEnabled = prefs.getBoolean(SettingsKeys.CLASS_DND_ENABLED, false)
        val leadMinutes =
            prefs.getInt(SettingsKeys.REMINDER_LEAD_MINUTES, ReminderTiming.LEAD_MINUTES)

        val rcs = mutableSetOf<Int>()
        var nextTriggerAtMs: Long? = null
        var nextTriggerRc: Int? = null

        for ((dayOffset, slots) in daySlots) {
            for ((index, slot) in slots.withIndex()) {
                val key = slotKey(dayOffset, slot.courseId, index)

                if (preEnabled) {
                    // 课间短于提前量：改在上一节下课时提醒
                    val prevEndMs =
                        slots.take(index)
                            .filter { it.endAtMs <= slot.startAtMs }
                            .maxOfOrNull { it.endAtMs }
                    val triggerAtMs =
                        if (prevEndMs != null &&
                            (slot.startAtMs - prevEndMs) / 60_000L < leadMinutes
                        ) {
                            prevEndMs
                        } else {
                            slot.startAtMs - leadMinutes * 60_000L
                        }

                    when {
                        triggerAtMs > nowMs -> {
                            val rc = RC_PRE_BASE + key
                            if (schedulePreClass(context, slot, rc, triggerAtMs, bookkeeping)) {
                                rcs += rc
                                if (nextTriggerAtMs == null || triggerAtMs < nextTriggerAtMs) {
                                    nextTriggerAtMs = triggerAtMs
                                    nextTriggerRc = rc
                                }
                            }
                        }

                        nowMs < slot.startAtMs -> sendPreClassIfNeeded(context, bookkeeping, slot)
                    }
                }

                // 课中提醒不再单独排上课闹钟：与 Nexio 一致，由实况/超级岛状态在分钟链里切换
                if (dndEnabled) {
                    if (slot.startAtMs > nowMs) {
                        val rc = RC_DND_START_BASE + key
                        if (scheduleDnd(
                                context,
                                rc,
                                ClassDndReceiver.ACTION_START,
                                slot.startAtMs,
                            )
                        ) {
                            rcs += rc
                        }
                    }
                    if (slot.endAtMs > nowMs) {
                        val rc = RC_DND_END_BASE + key
                        if (scheduleDnd(context, rc, ClassDndReceiver.ACTION_END, slot.endAtMs)) {
                            rcs += rc
                        }
                    }
                }
            }
        }

        // 测试课同样注册上课/下课闹钟：分钟级对账太粗，走精确闹钟才能准时开、准时关
        if (dndEnabled) {
            LiveReminderState.testClassWindow()?.let { (start, end) ->
                if (start > nowMs &&
                    scheduleDnd(context, RC_DND_TEST_START, ClassDndReceiver.ACTION_START, start)
                ) {
                    rcs += RC_DND_TEST_START
                }
                if (end > nowMs &&
                    scheduleDnd(context, RC_DND_TEST_END, ClassDndReceiver.ACTION_END, end)
                ) {
                    rcs += RC_DND_TEST_END
                }
            }
        }

        writeRegistry(bookkeeping, rcs)
        return DaySchedule(nextTriggerAtMs = nextTriggerAtMs, nextTriggerRc = nextTriggerRc)
    }

    /**
     * 测试课（「测试小米超级岛」/「测试实时活动」）启动后同步勿扰闹钟，
     * 让「上课自动开启 / 下课自动关闭」在测试里也能验证。
     */
    suspend fun syncTestClassDndAlarms(context: Context) {
        val prefs: ObservableSettings = get(named(SettingsKeys.PREFS_NAME))
        if (!prefs.getBoolean(SettingsKeys.CLASS_DND_ENABLED, false)) return
        val window = LiveReminderState.testClassWindow() ?: return
        scheduleDnd(context, RC_DND_TEST_START, ClassDndReceiver.ACTION_START, window.first)
        scheduleDnd(context, RC_DND_TEST_END, ClassDndReceiver.ACTION_END, window.second)
        // 与 Nexio 一致：排完立即按当前时间对账，上一轮测试残留的勿扰状态当场交还
        ClassDndController.applyCurrentState(context)
    }

    /** 取消全部课程类闹钟（课前 / 课中 / 勿扰），不动次日提醒。 */
    fun cancelCourseAlarms(context: Context, bookkeeping: ObservableSettings) {
        for (rc in readRegistry(bookkeeping)) {
            cancelRcByClass(context, rc)
        }
        writeRegistry(bookkeeping, emptySet())
        // 升级前固定 request code 的残留
        ReminderAlarmScheduler.cancel(context, RC_LEGACY_PRE, CourseReminderReceiver::class.java)
        ReminderAlarmScheduler.cancel(
            context,
            RC_LEGACY_DND_START,
            ClassDndReceiver::class.java,
            ClassDndReceiver.ACTION_START,
        )
        ReminderAlarmScheduler.cancel(
            context,
            RC_LEGACY_DND_END,
            ClassDndReceiver::class.java,
            ClassDndReceiver.ACTION_END,
        )
    }

    /** 次日提醒：每天在配置的时刻触发；排期闸门照 Nexio 的 shouldScheduleNextDayReminder。 */
    suspend fun refreshNextDay(
        context: Context,
        prefs: ObservableSettings,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        ReminderAlarmScheduler.cancel(context, RC_NEXT_DAY, NextDayReminderReceiver::class.java)
        if (!prefs.getBoolean(SettingsKeys.NEXT_DAY_REMINDER_ENABLED, false)) return
        if (!shouldScheduleNextDay(nowMs)) return

        val minuteOfDay =
            prefs.getInt(
                SettingsKeys.NEXT_DAY_REMINDER_MINUTE_OF_DAY,
                ReminderTiming.NEXT_DAY_MINUTE_OF_DAY,
            )
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
                set(Calendar.MINUTE, minuteOfDay % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        if (calendar.timeInMillis <= nowMs) calendar.add(Calendar.DAY_OF_YEAR, 1)

        ReminderAlarmScheduler.schedule(
            context,
            calendar.timeInMillis,
            PendingIntent.getBroadcast(
                context,
                RC_NEXT_DAY,
                Intent(context, NextDayReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    /**
     * 学期已开始：只在教学周内、且不超过最后一个有课周时排；
     * 学期未开始（假期）：只排**明天确有课**的那一天（返校前夜），避免整个假期每天空响。
     */
    private suspend fun shouldScheduleNextDay(nowMs: Long): Boolean {
        val courses = courseRepository.getAllCoursesOnce()
        val campus = timetable.getCampus()
        val termStartMs = timetable.getTermStartMs()
        val totalWeeks = timetable.getTotalWeeks()
        val weekInfo =
            WeekCalculator.detect(
                totalWeeks = totalWeeks,
                termStartMs = termStartMs,
                nowMs = nowMs,
            )
        if (weekInfo.isHoliday) {
            return CourseReminderPlanner.planDay(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = nowMs,
                dayOffset = 1,
            ).isNotEmpty()
        }
        val lastWeek = lastWeekWithCourses(courses)
        return lastWeek == 0 || weekInfo.week <= lastWeek
    }

    private fun lastWeekWithCourses(courses: List<Course>): Int {
        var last = 0
        for (course in courses) {
            val week = if (course.type == 2) course.targetWeek else course.weeks.maxOrNull() ?: 0
            if (week > last) last = week
        }
        return last
    }

    /**
     * 闹钟触发时按课程 id 回查当前课表（今天或明天的课，覆盖凌晨课）。
     * 查不到说明课程已删除或改动，调用方应跳过发送并整批重排。
     */
    suspend fun resolveSlotById(courseId: Long, nowMs: Long): CourseSlot? {
        if (courseId <= 0L) return null
        val courses = courseRepository.getAllCoursesOnce()
        val campus = timetable.getCampus()
        val termStartMs = timetable.getTermStartMs()
        val totalWeeks = timetable.getTotalWeeks()
        val today =
            CourseReminderPlanner.planDay(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = nowMs,
                dayOffset = 0,
            )
        val tomorrow =
            CourseReminderPlanner.planDay(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = nowMs,
                dayOffset = 1,
            )
        return (today + tomorrow).firstOrNull { it.courseId == courseId }
    }

    // ————————————————————————— 发送与去重 —————————————————————————

    /** 闹钟已过点但还没上课：立即补发；同一课次 60 分钟内只发一次。 */
    fun sendPreClassIfNeeded(
        context: Context,
        bookkeeping: ObservableSettings,
        slot: CourseSlot,
    ) {
        if (wasPreClassSent(bookkeeping, slot)) return
        postCourseReminderNotification(
            context = context,
            name = slot.name,
            location = slot.place,
            time = slot.timeStart,
            endTime = slot.timeEnd,
        )
        markPreClassSent(bookkeeping, slot)
    }

    fun markPreClassSent(
        bookkeeping: ObservableSettings,
        slot: CourseSlot,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val map = sentMap(bookkeeping, KEY_PRE_SENT_DAY, KEY_PRE_SENT_MAP, nowMs)
        map.put(slotDedupId(slot), nowMs)
        writeSentMap(bookkeeping, KEY_PRE_SENT_DAY, KEY_PRE_SENT_MAP, map, nowMs)
    }

    fun wasPreClassSent(
        bookkeeping: ObservableSettings,
        slot: CourseSlot,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val last =
            sentMap(bookkeeping, KEY_PRE_SENT_DAY, KEY_PRE_SENT_MAP, nowMs)
                .optLong(slotDedupId(slot), 0L)
        return last > 0L && nowMs - last < PRE_SENT_DEDUP_WINDOW_MS
    }

    private fun slotDedupId(slot: CourseSlot): String =
        "${slot.name}|${slot.timeStart}|${slot.startAtMs}"

    /** 当天的发送记录；跨日整份作废，保证次日可重新提醒。 */
    private fun sentMap(
        bookkeeping: ObservableSettings,
        dayKey: String,
        mapKey: String,
        nowMs: Long,
    ): JSONObject {
        if (bookkeeping.getString(dayKey, "") != dayString(nowMs)) return JSONObject()
        return try {
            JSONObject(bookkeeping.getString(mapKey, ""))
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun writeSentMap(
        bookkeeping: ObservableSettings,
        dayKey: String,
        mapKey: String,
        map: JSONObject,
        nowMs: Long,
    ) {
        bookkeeping[dayKey] = dayString(nowMs)
        bookkeeping[mapKey] = map.toString()
    }

    private fun dayString(nowMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))

    // ————————————————————————— 具体排期 —————————————————————————

    private fun schedulePreClass(
        context: Context,
        slot: CourseSlot,
        rc: Int,
        triggerAtMs: Long,
        bookkeeping: ObservableSettings,
    ): Boolean {
        val intent =
            Intent(context, CourseReminderReceiver::class.java).apply {
                putExtra(EXTRA_COURSE_ID, slot.courseId)
                putExtra(EXTRA_START_AT, slot.startAtMs)
                putExtra(EXTRA_NAME, slot.name)
                putExtra(EXTRA_PLACE, slot.place)
                putExtra(EXTRA_START, slot.timeStart)
                putExtra(EXTRA_END, slot.timeEnd)
            }
        return ReminderAlarmScheduler.scheduleBoundary(
            context,
            triggerAtMs,
            PendingIntent.getBroadcast(
                context,
                rc,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    private fun scheduleDnd(
        context: Context,
        rc: Int,
        action: String,
        triggerAtMs: Long,
    ): Boolean =
        ReminderAlarmScheduler.scheduleBoundary(
            context,
            triggerAtMs,
            PendingIntent.getBroadcast(
                context,
                rc,
                Intent(context, ClassDndReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

    // ————————————————————————— 登记表 —————————————————————————

    private fun slotKey(dayOffset: Int, courseId: Long, index: Int): Int {
        val withinDay =
            if (courseId > 0L) {
                (courseId % SLOTS_PER_DAY).toInt()
            } else {
                SLOTS_PER_DAY - 1 - index
            }
        return dayOffset.coerceIn(0, 1) * SLOTS_PER_DAY + withinDay
    }

    private fun cancelRcByClass(context: Context, rc: Int) {
        when (rc) {
            RC_DND_TEST_START ->
                ReminderAlarmScheduler.cancel(
                    context,
                    rc,
                    ClassDndReceiver::class.java,
                    ClassDndReceiver.ACTION_START,
                )
            RC_DND_TEST_END ->
                ReminderAlarmScheduler.cancel(
                    context,
                    rc,
                    ClassDndReceiver::class.java,
                    ClassDndReceiver.ACTION_END,
                )
            in RC_PRE_BASE until RC_IN_CLASS_BASE ->
                ReminderAlarmScheduler.cancel(context, rc, CourseReminderReceiver::class.java)
            in RC_DND_START_BASE until RC_DND_END_BASE ->
                ReminderAlarmScheduler.cancel(
                    context,
                    rc,
                    ClassDndReceiver::class.java,
                    ClassDndReceiver.ACTION_START,
                )
            in RC_DND_END_BASE until RC_DND_END_BASE + SLOTS_PER_DAY * 2 ->
                ReminderAlarmScheduler.cancel(
                    context,
                    rc,
                    ClassDndReceiver::class.java,
                    ClassDndReceiver.ACTION_END,
                )
        }
    }

    private fun readRegistry(bookkeeping: ObservableSettings): Set<Int> =
        bookkeeping.getString(KEY_REGISTRY, "")
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

    private fun writeRegistry(bookkeeping: ObservableSettings, rcs: Set<Int>) {
        bookkeeping[KEY_REGISTRY] = rcs.joinToString(",")
    }
}
