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
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextCourseReminderIfEnabledUseCase
import restarhalf.stellar.schedule.reminder.ClassDndController
import restarhalf.stellar.schedule.reminder.CourseDayAlarmScheduler
import restarhalf.stellar.schedule.reminder.NotificationChannels
import restarhalf.stellar.schedule.reminder.PreClassLiveDispatcher
import restarhalf.stellar.schedule.reminder.ReminderTickScheduler
import java.util.Calendar

/**
 * 课前提醒闹钟。
 *
 * 与 Nexio 相同：闹钟里带的是注册时刻的课程快照，触发时一律回查当前课表：
 * 课程被删 / 改时间就按新课表发，查不到就跳过并整批重排清理过期闹钟；
 * 同一课次只发一次（闹钟与补发路径共用去重标记）。
 */
class CourseReminderReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val courseId = intent.getLongExtra(CourseDayAlarmScheduler.EXTRA_COURSE_ID, 0L)

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                NotificationChannels.ensureAll(context)
                val deps = Deps()
                val nowMs = System.currentTimeMillis()
                val slot = CourseDayAlarmScheduler.resolveSlotById(courseId, nowMs)
                // 与 Nexio 的 onAlarmProcessed 一致：闹钟在调度时已一次性注册，
                // 正常触发只做轻量收尾；只有课程已删除/改时间才整批重排清理过期闹钟。
                var fullReschedule = false
                when {
                    slot != null -> {
                        if (!CourseDayAlarmScheduler.wasPreClassSent(deps.bookkeeping, slot)) {
                            postCourseReminderNotification(
                                context = context,
                                name = slot.name,
                                location = slot.place,
                                time = slot.timeStart,
                                endTime = slot.timeEnd,
                            )
                            CourseDayAlarmScheduler.markPreClassSent(deps.bookkeeping, slot)
                        }
                    }

                    courseId <= 0L -> {
                        // 旧版闹钟没有 courseId：按快照照发一次
                        val name = intent.getStringExtra(CourseDayAlarmScheduler.EXTRA_NAME)
                        if (!name.isNullOrBlank()) {
                            postCourseReminderNotification(
                                context = context,
                                name = name,
                                location =
                                    intent.getStringExtra(CourseDayAlarmScheduler.EXTRA_PLACE)
                                        .orEmpty(),
                                time =
                                    intent.getStringExtra(CourseDayAlarmScheduler.EXTRA_START)
                                        .orEmpty(),
                                endTime =
                                    intent.getStringExtra(CourseDayAlarmScheduler.EXTRA_END)
                                        .orEmpty(),
                            )
                        }
                        fullReschedule = true
                    }

                    else -> fullReschedule = true
                }
                if (fullReschedule) {
                    deps.rescheduleNextReminderIfEnabled()
                } else {
                    ReminderTickScheduler.scheduleNext(context, nowMs)
                    ClassDndController.applyCurrentState(context, nowMs)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("CourseReminder", "处理课前提醒失败 courseId=$courseId", e)
            } finally {
                result.finish()
            }
        }
    }

    private class Deps : KoinComponent {
        val bookkeeping: ObservableSettings by inject(named("reminder_codes"))
        val rescheduleNextReminderIfEnabled: RescheduleNextCourseReminderIfEnabledUseCase by inject()
    }
}

/**
 * 课前提醒：与 Nexio 的 sendPreClassNotification 一致，只由超级岛或实况承载，
 * 不再发旧的静态提醒卡（静音按钮一并废弃，改用岛/实况上的「上课勿扰」）。
 */
internal fun postCourseReminderNotification(
    context: Context,
    name: String,
    location: String,
    time: String,
    endTime: String,
) {
    val startAt = parseTodayTimeMs(time) ?: return
    val endAt = if (endTime.isBlank()) null else parseTodayTimeMs(endTime)
    PreClassLiveDispatcher.dispatch(
        context = context,
        courseName = name,
        classroom = location,
        startTime = time,
        endTime = endTime,
        startMillis = startAt,
        endMillis = endAt ?: 0L,
    )
}

/** "HH:mm" → 今天该时刻的毫秒值。 */
internal fun parseTodayTimeMs(time: String): Long? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].trim().toIntOrNull() ?: return null
    val minute = parts[1].trim().toIntOrNull() ?: return null

    val calendar =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    return calendar.timeInMillis
}