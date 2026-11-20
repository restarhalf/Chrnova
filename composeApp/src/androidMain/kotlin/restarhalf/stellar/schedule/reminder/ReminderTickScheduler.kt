package restarhalf.stellar.schedule.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.russhwolf.settings.ObservableSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.reminder.CourseReminderPlanner
import restarhalf.stellar.schedule.domain.reminder.ExamReminderPlanner
import restarhalf.stellar.schedule.domain.reminder.ReminderTiming
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.reminder.receiver.ReminderTickReceiver
import java.util.Calendar

/**
 * 分钟级对账链，照 Nexio 的 scheduleWidgetRefresh / computeNextWidgetRefreshTime：
 *
 * - 每次触发后由 [ReminderTickReceiver] 重新注册下一次，链式调度；
 * - 有课/考试进行中或临近开始：下一分钟整点（并对齐开始瞬间）；
 * - 否则退到下次开始前 5 分钟，最迟 30 分钟一次；
 * - 一律钳在零点前，跨日时由接收器触发全量重排。
 *
 * 课程与考试共用同一条链，两边的窗口都参与下一次触发的计算。
 *
 * 链条必须始终精确唤醒，不能用 setAndAllowWhileIdle 降级：链式调度一旦掺入
 * 非精确闹钟，触发点漂移会逐次累积，跨日重排与课前补发整体失效。
 */
internal object ReminderTickScheduler : KoinComponent {

    private val courseRepository: CourseRepository by inject()
    private val examinationRepository: ExaminationRepository by inject()
    private val timetable: TimetablePort by inject()
    private val prefs: ObservableSettings by inject(named(SettingsKeys.PREFS_NAME))
    private val courseReminderScheduler: CourseReminderScheduler by inject()
    private val examReminderScheduler: ExamReminderScheduler by inject()

    const val ACTION_TICK = "restarhalf.stellar.schedule.action.REMINDER_TICK"

    private const val RC_TICK = 25001

    private const val FALLBACK_PERIOD_MS = 30L * 60_000L

    private const val EARLY_REFRESH_MS = 5L * 60_000L

    fun cancel(context: Context) {
        ReminderAlarmScheduler.cancel(context, RC_TICK, ReminderTickReceiver::class.java, ACTION_TICK)
    }

    suspend fun scheduleNext(
        context: Context,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                RC_TICK,
                Intent(context, ReminderTickReceiver::class.java).setAction(ACTION_TICK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        ReminderAlarmScheduler.schedule(
            context = context,
            triggerAtMs = computeNextTickMs(nowMs),
            pendingIntent = pendingIntent,
        )
    }

    private suspend fun computeNextTickMs(nowMs: Long): Long {
        val courseSlots =
            runCatching {
                CourseReminderPlanner.planDay(
                    courses = courseRepository.getAllCoursesOnce(),
                    campus = timetable.getCampus(),
                    termStartMs = timetable.getTermStartMs(),
                    totalWeeks = timetable.getTotalWeeks(),
                    nowMs = nowMs,
                    dayOffset = 0,
                )
            }.getOrDefault(emptyList())
        val examSlots =
            runCatching {
                ExamReminderPlanner.planDay(
                    exams = examinationRepository.getAllExaminationsOnce(),
                    nowMs = nowMs,
                    dayOffset = 0,
                )
            }.getOrDefault(emptyList())

        val leadMinutes =
            prefs.getInt(SettingsKeys.REMINDER_LEAD_MINUTES, ReminderTiming.LEAD_MINUTES)

        var nearSession = false
        var imminentStartMs: Long? = null
        var nextStartMs: Long? = null

        // 课程与考试的时间窗一视同仁
        val windows =
            courseSlots.map { it.startAtMs to it.endAtMs } +
                examSlots.map { it.startAtMs to it.endAtMs }

        for ((startAtMs, endAtMs) in windows.sortedBy { it.first }) {
            // 进行中：倒计时/状态文案只有跨分钟才会变
            if (nowMs >= startAtMs && nowMs < endAtMs) {
                nearSession = true
                break
            }
            // 提醒窗口内保持每分钟刷新，保证补发与倒计时都被驱动
            if (nowMs >= startAtMs - leadMinutes * 60_000L && nowMs < startAtMs) {
                nearSession = true
                imminentStartMs = startAtMs
                break
            }
            if (startAtMs > nowMs) {
                if (nextStartMs == null || startAtMs < nextStartMs) nextStartMs = startAtMs
            }
        }

        // 实况倒计时/超级岛/进行中进行中：必须每分钟驱动更新与对账，与是否还有场次在数据里无关。
        // 测试课（测试超级岛 / 测试实时活动）也在这里续链，否则测试课的进度与对账没人驱动
        val liveActive =
            ReminderSession.entries.any {
                LiveReminderState.isCountdownActive(it) || LiveReminderState.islandSnapshot(it) != null
            }

        val result =
            if (nearSession || liveActive) {
                val nextMinute =
                    Calendar.getInstance().apply {
                        timeInMillis = nowMs
                        add(Calendar.MINUTE, 1)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                // 对齐开始瞬间，避免倒计时归零后等一整分钟才切「已开始」
                val start = imminentStartMs
                if (start != null && start > nowMs) minOf(nextMinute, start + 1_000L) else nextMinute
            } else if (nextStartMs != null) {
                minOf(nextStartMs - EARLY_REFRESH_MS, nowMs + FALLBACK_PERIOD_MS)
            } else {
                nowMs + FALLBACK_PERIOD_MS
            }

        return capAtMidnight(result, nowMs)
    }

    /** 钳到 now+1s 与下一个零点之间，防止过去时间立即触发连刷、也保证跨日一定重排。 */
    private fun capAtMidnight(result: Long, nowMs: Long): Long {
        val nextMidnight =
            Calendar.getInstance().apply {
                timeInMillis = nowMs
                add(Calendar.DATE, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        return maxOf(minOf(result, nextMidnight), nowMs + 1_000L)
    }

    /** 跨日检测：日期变化时课程与考试都整批重排（覆盖次日场次无提醒的场景）。 */
    suspend fun rescheduleIfDayChanged(context: Context) {
        if (!courseReminderScheduler.isScheduledForToday()) {
            courseReminderScheduler.scheduleNextReminder(
                courses = courseRepository.getAllCoursesOnce(),
                campus = timetable.getCampus(),
                termStartMs = timetable.getTermStartMs(),
                totalWeeks = timetable.getTotalWeeks(),
            )
        }
        if (!examReminderScheduler.isScheduledForToday()) {
            examReminderScheduler.scheduleNextReminder(
                exams = examinationRepository.getAllExaminationsOnce(),
            )
        }
    }

    /**
     * 兜底补发：闹钟未触发/丢失时，在提醒窗口内立即补上。
     * 窗口与 scheduleDays 一致：前一场结束晚于提前点时从前一场结束算起。
     */
    suspend fun catchUpPendingPreClass(context: Context) {
        if (!prefs.getBoolean(SettingsKeys.REMINDER_ENABLED, false)) return
        val nowMs = System.currentTimeMillis()
        val courseSlots =
            runCatching {
                CourseReminderPlanner.planDay(
                    courses = courseRepository.getAllCoursesOnce(),
                    campus = timetable.getCampus(),
                    termStartMs = timetable.getTermStartMs(),
                    totalWeeks = timetable.getTotalWeeks(),
                    nowMs = nowMs,
                    dayOffset = 0,
                )
            }.getOrDefault(emptyList())
        val examSlots =
            runCatching {
                ExamReminderPlanner.planDay(
                    exams = examinationRepository.getAllExaminationsOnce(),
                    nowMs = nowMs,
                    dayOffset = 0,
                )
            }.getOrDefault(emptyList())
        if (courseSlots.isEmpty() && examSlots.isEmpty()) return

        val leadMinutes =
            prefs.getInt(SettingsKeys.REMINDER_LEAD_MINUTES, ReminderTiming.LEAD_MINUTES)
        val bookkeeping =
            runCatching { get<ObservableSettings>(named("reminder_codes")) }.getOrNull() ?: return

        for ((index, slot) in courseSlots.withIndex()) {
            val prevEndMs =
                courseSlots.take(index)
                    .filter { it.endAtMs <= slot.startAtMs }
                    .maxOfOrNull { it.endAtMs }
            val triggerAtMs =
                if (prevEndMs != null && (slot.startAtMs - prevEndMs) / 60_000L < leadMinutes) {
                    prevEndMs
                } else {
                    slot.startAtMs - leadMinutes * 60_000L
                }
            if (nowMs >= triggerAtMs && nowMs < slot.startAtMs) {
                CourseDayAlarmScheduler.sendPreClassIfNeeded(context, bookkeeping, slot)
            }
        }

        for ((index, slot) in examSlots.withIndex()) {
            val prevEndMs =
                examSlots.take(index)
                    .filter { it.endAtMs <= slot.startAtMs }
                    .maxOfOrNull { it.endAtMs }
            val triggerAtMs =
                if (prevEndMs != null && (slot.startAtMs - prevEndMs) / 60_000L < leadMinutes) {
                    prevEndMs
                } else {
                    slot.startAtMs - leadMinutes * 60_000L
                }
            if (nowMs >= triggerAtMs && nowMs < slot.startAtMs) {
                ExamDayAlarmScheduler.sendPreExamIfNeeded(context, bookkeeping, slot)
            }
        }
    }
}