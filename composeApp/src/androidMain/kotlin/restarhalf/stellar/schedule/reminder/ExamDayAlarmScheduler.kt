package restarhalf.stellar.schedule.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import org.json.JSONObject
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.reminder.ExamReminderPlanner
import restarhalf.stellar.schedule.domain.reminder.ExamSlot
import restarhalf.stellar.schedule.domain.reminder.ReminderTiming
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.reminder.receiver.ExamDndReceiver
import restarhalf.stellar.schedule.reminder.receiver.ExamReminderReceiver
import restarhalf.stellar.schedule.reminder.receiver.postExamReminderNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 考试相关闹钟，排法与 [CourseDayAlarmScheduler] 完全一致：
 *
 * - 不是「只排下一条」，而是把窗口内每一场考试都排上：
 *   考前提醒、开考/结束两个勿扰边界各一条，
 *   全部用考试 id 派生的 request code，并在登记表里记下，取消时按登记表清，
 *   考试被删也不会留下孤儿闹钟。
 * - 考前提醒遇上前一场结束晚于提前点时，改在前一场结束时提醒，避免落进上一场考试里。
 * - 触发点已过但还没开考的（闹钟丢失 / Doze 延迟 / 刚打开应用），立即补发，按场次去重。
 * - 边界闹钟走 setAlarmClock，Doze 下也能准点。
 */
internal object ExamDayAlarmScheduler : KoinComponent {

    private val examinationRepository: ExaminationRepository by inject()

    private const val RC_PRE_BASE = 500_000
    private const val RC_DND_START_BASE = 600_000
    private const val RC_DND_END_BASE = 700_000

    /** 旧「单条考试提醒」固定 request code，升级后清理残留闹钟 */
    private const val RC_LEGACY_EXAM = 21001

    private const val KEY_REGISTRY = "exam_alarm_rcs"

    // 发送去重照课程：按场次 id 记时间戳的 JSON map，跨日整份失效。
    // 考前提醒 60 分钟内不重发。
    private const val KEY_PRE_SENT_DAY = "exam_pre_sent_day"
    private const val KEY_PRE_SENT_MAP = "exam_pre_sent_map"
    private const val PRE_SENT_DEDUP_WINDOW_MS = 60 * 60 * 1000L

    /** 每天可用的 request code 槽位数；两天同时排也不冲突 */
    private const val SLOTS_PER_DAY = 40_000

    const val EXTRA_EXAM_ID = "extra_exam_id"
    const val EXTRA_START_AT = "extra_exam_start_at"
    const val EXTRA_NAME = "extra_exam_name"
    const val EXTRA_PLACE = "extra_exam_place"
    const val EXTRA_START = "extra_exam_start"
    const val EXTRA_END = "extra_exam_end"

    /**
     * 排 [daySlots]（dayOffset 与当天考试）的全部闹钟。
     * 返回最早的一条考前提醒，供「下次提醒」摘要与 hasScheduledAlarm 使用。
     */
    fun scheduleDays(
        context: Context,
        bookkeeping: ObservableSettings,
        prefs: ObservableSettings,
        daySlots: List<Pair<Int, List<ExamSlot>>>,
        nowMs: Long,
    ): DaySchedule {
        cancelExamAlarms(context, bookkeeping)

        val preEnabled = prefs.getBoolean(SettingsKeys.REMINDER_ENABLED, false)
        val dndEnabled = prefs.getBoolean(SettingsKeys.EXAM_DND_ENABLED, false)
        val leadMinutes =
            prefs.getInt(SettingsKeys.REMINDER_LEAD_MINUTES, ReminderTiming.LEAD_MINUTES)

        val rcs = mutableSetOf<Int>()
        var nextTriggerAtMs: Long? = null
        var nextTriggerRc: Int? = null

        for ((dayOffset, slots) in daySlots) {
            for ((index, slot) in slots.withIndex()) {
                val key = slotKey(dayOffset, slot.examId, index)

                if (preEnabled) {
                    // 前一场结束晚于提前点：改在前一场结束时提醒
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
                            if (schedulePreExam(context, slot, rc, triggerAtMs)) {
                                rcs += rc
                                if (nextTriggerAtMs == null || triggerAtMs < nextTriggerAtMs) {
                                    nextTriggerAtMs = triggerAtMs
                                    nextTriggerRc = rc
                                }
                            }
                        }

                        nowMs < slot.startAtMs -> sendPreExamIfNeeded(context, bookkeeping, slot)
                    }
                }

                // 考中提醒不单独排开考闹钟：与课程一致，由实况/超级岛状态在分钟链里切换
                if (dndEnabled) {
                    if (slot.startAtMs > nowMs) {
                        val rc = RC_DND_START_BASE + key
                        if (scheduleDnd(
                                context,
                                rc,
                                ExamDndReceiver.ACTION_START,
                                slot.startAtMs,
                            )
                        ) {
                            rcs += rc
                        }
                    }
                    if (slot.endAtMs > nowMs) {
                        val rc = RC_DND_END_BASE + key
                        if (scheduleDnd(context, rc, ExamDndReceiver.ACTION_END, slot.endAtMs)) {
                            rcs += rc
                        }
                    }
                }
            }
        }

        writeRegistry(bookkeeping, rcs)
        return DaySchedule(nextTriggerAtMs = nextTriggerAtMs, nextTriggerRc = nextTriggerRc)
    }

    /** 取消全部考试类闹钟（考前 / 勿扰），照 cancelCourseAlarms。 */
    fun cancelExamAlarms(context: Context, bookkeeping: ObservableSettings) {
        for (rc in readRegistry(bookkeeping)) {
            cancelRcByClass(context, rc)
        }
        writeRegistry(bookkeeping, emptySet())
        // 升级前固定 request code 的残留
        ReminderAlarmScheduler.cancel(context, RC_LEGACY_EXAM, ExamReminderReceiver::class.java)
    }

    /**
     * 闹钟触发时按考试 id 回查当前考试安排（今天或明天的场次，覆盖凌晨场）。
     * 查不到说明考试已删除或改动，调用方应跳过发送并整批重排。
     */
    suspend fun resolveSlotById(examId: Long, nowMs: Long): ExamSlot? {
        if (examId <= 0L) return null
        val exams = examinationRepository.getAllExaminationsOnce()
        val today = ExamReminderPlanner.planDay(exams = exams, nowMs = nowMs, dayOffset = 0)
        val tomorrow = ExamReminderPlanner.planDay(exams = exams, nowMs = nowMs, dayOffset = 1)
        return (today + tomorrow).firstOrNull { it.examId == examId }
    }

    // ————————————————————————— 发送与去重 —————————————————————————

    /** 闹钟已过点但还没开考：立即补发；同一场次 60 分钟内只发一次。 */
    fun sendPreExamIfNeeded(
        context: Context,
        bookkeeping: ObservableSettings,
        slot: ExamSlot,
    ) {
        if (wasPreExamSent(bookkeeping, slot)) return
        postExamReminderNotification(
            context = context,
            name = slot.name,
            place = slot.place,
            startTime = slot.timeStart,
            endTime = slot.timeEnd,
            startAtMs = slot.startAtMs,
            endAtMs = slot.endAtMs,
        )
        markPreExamSent(bookkeeping, slot)
    }

    fun markPreExamSent(
        bookkeeping: ObservableSettings,
        slot: ExamSlot,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val map = sentMap(bookkeeping, nowMs)
        map.put(slotDedupId(slot), nowMs)
        writeSentMap(bookkeeping, map, nowMs)
    }

    fun wasPreExamSent(
        bookkeeping: ObservableSettings,
        slot: ExamSlot,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val last = sentMap(bookkeeping, nowMs).optLong(slotDedupId(slot), 0L)
        return last > 0L && nowMs - last < PRE_SENT_DEDUP_WINDOW_MS
    }

    private fun slotDedupId(slot: ExamSlot): String =
        "${slot.name}|${slot.timeStart}|${slot.startAtMs}"

    /** 当天的发送记录；跨日整份作废，保证次日可重新提醒。 */
    private fun sentMap(bookkeeping: ObservableSettings, nowMs: Long): JSONObject {
        if (bookkeeping.getString(KEY_PRE_SENT_DAY, "") != dayString(nowMs)) return JSONObject()
        return try {
            JSONObject(bookkeeping.getString(KEY_PRE_SENT_MAP, ""))
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun writeSentMap(
        bookkeeping: ObservableSettings,
        map: JSONObject,
        nowMs: Long,
    ) {
        bookkeeping[KEY_PRE_SENT_DAY] = dayString(nowMs)
        bookkeeping[KEY_PRE_SENT_MAP] = map.toString()
    }

    private fun dayString(nowMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))

    // ————————————————————————— 具体排期 —————————————————————————

    private fun schedulePreExam(
        context: Context,
        slot: ExamSlot,
        rc: Int,
        triggerAtMs: Long,
    ): Boolean {
        val intent =
            Intent(context, ExamReminderReceiver::class.java).apply {
                putExtra(EXTRA_EXAM_ID, slot.examId)
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
                Intent(context, ExamDndReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

    // ————————————————————————— 登记表 —————————————————————————

    private fun slotKey(dayOffset: Int, examId: Long, index: Int): Int {
        val withinDay =
            if (examId > 0L) {
                (examId % SLOTS_PER_DAY).toInt()
            } else {
                SLOTS_PER_DAY - 1 - index
            }
        return dayOffset.coerceIn(0, 1) * SLOTS_PER_DAY + withinDay
    }

    private fun cancelRcByClass(context: Context, rc: Int) {
        when (rc) {
            in RC_PRE_BASE until RC_DND_START_BASE ->
                ReminderAlarmScheduler.cancel(context, rc, ExamReminderReceiver::class.java)
            in RC_DND_START_BASE until RC_DND_END_BASE ->
                ReminderAlarmScheduler.cancel(
                    context,
                    rc,
                    ExamDndReceiver::class.java,
                    ExamDndReceiver.ACTION_START,
                )
            in RC_DND_END_BASE until RC_DND_END_BASE + SLOTS_PER_DAY * 2 ->
                ReminderAlarmScheduler.cancel(
                    context,
                    rc,
                    ExamDndReceiver::class.java,
                    ExamDndReceiver.ACTION_END,
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