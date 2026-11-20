package restarhalf.stellar.schedule.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextExamReminderIfEnabledUseCase
import restarhalf.stellar.schedule.reminder.ExamDayAlarmScheduler
import restarhalf.stellar.schedule.reminder.ExamDndController
import restarhalf.stellar.schedule.reminder.NotificationChannels
import restarhalf.stellar.schedule.reminder.PreClassLiveDispatcher
import restarhalf.stellar.schedule.reminder.ReminderSession
import restarhalf.stellar.schedule.reminder.ReminderTickScheduler

/**
 * 考前提醒闹钟，与 [CourseReminderReceiver] 完全一致：
 *
 * 闹钟里带的是注册时刻的考试快照，触发时一律回查当前考试安排：
 * 考试被删 / 改时间就按新安排发，查不到就跳过并整批重排清理过期闹钟；
 * 同一场次只发一次（闹钟与补发路径共用去重标记）。
 */
class ExamReminderReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val examId = intent.getLongExtra(ExamDayAlarmScheduler.EXTRA_EXAM_ID, 0L)

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                NotificationChannels.ensureAll(context)
                val deps = Deps()
                val nowMs = System.currentTimeMillis()
                val slot = ExamDayAlarmScheduler.resolveSlotById(examId, nowMs)
                // 与课程的 onAlarmProcessed 一致：闹钟在调度时已一次性注册，
                // 正常触发只做轻量收尾；只有考试已删除/改时间才整批重排清理过期闹钟。
                var fullReschedule = false
                when {
                    slot != null -> {
                        if (!ExamDayAlarmScheduler.wasPreExamSent(deps.bookkeeping, slot)) {
                            postExamReminderNotification(
                                context = context,
                                name = slot.name,
                                place = slot.place,
                                startTime = slot.timeStart,
                                endTime = slot.timeEnd,
                                startAtMs = slot.startAtMs,
                                endAtMs = slot.endAtMs,
                            )
                            ExamDayAlarmScheduler.markPreExamSent(deps.bookkeeping, slot)
                        }
                    }

                    examId <= 0L -> {
                        // 旧版闹钟没有 examId：按快照照发一次（与课程的旧闹钟分支一致）
                        val name = intent.getStringExtra(LEGACY_EXTRA_COURSE_NAME)
                        if (!name.isNullOrBlank()) {
                            val examTime = intent.getStringExtra(LEGACY_EXTRA_EXAM_TIME).orEmpty()
                            val period = parseExamPeriod(examTime)
                            val startAt = period?.let { parseTodayTimeMs(it.first) }
                            if (startAt != null) {
                                postExamReminderNotification(
                                    context = context,
                                    name = name,
                                    place = intent.getStringExtra(LEGACY_EXTRA_PLACE).orEmpty(),
                                    startTime = period.first,
                                    endTime = period.second,
                                    startAtMs = startAt,
                                    endAtMs = parseTodayTimeMs(period.second) ?: 0L,
                                )
                            }
                        }
                        fullReschedule = true
                    }

                    else -> fullReschedule = true
                }
                if (fullReschedule) {
                    deps.rescheduleNextReminderIfEnabled()
                } else {
                    ReminderTickScheduler.scheduleNext(context, nowMs)
                    ExamDndController.applyCurrentState(context, nowMs)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("ExamReminder", "处理考前提醒失败 examId=$examId", e)
            } finally {
                result.finish()
            }
        }
    }

    private class Deps : KoinComponent {
        val bookkeeping: ObservableSettings by inject(named("reminder_codes"))
        val rescheduleNextReminderIfEnabled: RescheduleNextExamReminderIfEnabledUseCase by inject()
    }

    private companion object {
        // 旧版「单条考试提醒」闹钟（RC 21001）的快照 extra，升级残留触发时兜底用
        const val LEGACY_EXTRA_COURSE_NAME = "exam_course_name"
        const val LEGACY_EXTRA_PLACE = "exam_place"
        const val LEGACY_EXTRA_EXAM_TIME = "exam_time"
    }
}

/** 旧版考试时间的 "HH:mm~HH:mm" 段；解析不出返回 null。 */
private fun parseExamPeriod(examTime: String): Pair<String, String>? {
    val match =
        Regex("(\\d{1,2}:\\d{2})\\s*~\\s*(\\d{1,2}:\\d{2})").find(examTime) ?: return null
    return match.groupValues[1] to match.groupValues[2]
}

/**
 * 考前提醒：与课程的 postCourseReminderNotification 一致，只由超级岛或实况承载，
 * 走考试会话（考试文案、考试勿扰、考试中进度）。
 */
internal fun postExamReminderNotification(
    context: Context,
    name: String,
    place: String,
    startTime: String,
    endTime: String,
    startAtMs: Long,
    endAtMs: Long,
) {
    PreClassLiveDispatcher.dispatch(
        context = context,
        courseName = name,
        classroom = place,
        startTime = startTime,
        endTime = endTime,
        startMillis = startAtMs,
        endMillis = endAtMs,
        session = ReminderSession.Exam,
    )
}