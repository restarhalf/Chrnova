package restarhalf.stellar.schedule.reminder.receiver

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresApi
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.MainActivity
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.reminder.CourseReminderPlanner
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.reminder.ClassDndController
import restarhalf.stellar.schedule.reminder.CourseDayAlarmScheduler
import restarhalf.stellar.schedule.reminder.ExamDndController
import restarhalf.stellar.schedule.reminder.NotificationChannels

internal fun safeNotify(context: Context, id: Int, channelId: String, title: String, text: String) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val contentIntent =
        PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    val notification =
        Notification.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    manager.notify(id, notification)
}

class NextDayReminderReceiver : BroadcastReceiver(), KoinComponent {

    private class Deps : KoinComponent {
        val courseRepository: CourseRepository by inject()
        val timetable: TimetablePort by inject()
    }

    @RequiresApi(android.os.Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val settings: ObservableSettings = get(named(SettingsKeys.PREFS_NAME))
        if (!settings.getBoolean(SettingsKeys.NEXT_DAY_REMINDER_ENABLED, false)) {
            result.finish()
            return
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                NotificationChannels.ensureAll(context)
                val deps = Deps()
                val nowMs = System.currentTimeMillis()
                val summary =
                    CourseReminderPlanner.summarizeDay(
                        courses = deps.courseRepository.getAllCoursesOnce(),
                        campus = deps.timetable.getCampus(),
                        termStartMs = deps.timetable.getTermStartMs(),
                        totalWeeks = deps.timetable.getTotalWeeks(),
                        nowMs = nowMs,
                        dayOffset = 1,
                    )
                if (summary != null) {
                    val text = buildString {
                        append("明天 ${summary.firstStart} 上「${summary.firstCourseName}」")
                        if (summary.firstPlace.isNotBlank()) append(" · ${summary.firstPlace}")
                        if (summary.count > 1) append("，共 ${summary.count} 节课")
                    }
                    safeNotify(
                        context,
                        NEXT_DAY_ID,
                        NotificationChannels.NEXT_DAY_CHANNEL_ID,
                        "明日课程提醒",
                        text,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                CourseDayAlarmScheduler.refreshNextDay(context, settings)
                result.finish()
            }
        }
    }

    companion object {
        private const val NEXT_DAY_ID = 23001
    }
}

/**
 * 上课/下课勿扰边界闹钟。
 *
 * 与 Nexio 相同：接收器不直接「进入/退出」，而是按当前时间对账 ——
 * 闹钟迟到、丢失或课表刚被修改时，对账结果都正确。
 */
class ClassDndReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val toggle = intent.action == ACTION_TOGGLE
        if (!toggle && intent.action != ACTION_START && intent.action != ACTION_END) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (toggle) {
                    // 通知 / 超级岛上的「上课勿扰」按钮：与 Nexio 一致，点一下立即切换
                    ClassDndController.toggleFromNotification(context)
                } else {
                    ClassDndController.applyCurrentState(context)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_START = "restarhalf.schedule.DND_START"
        const val ACTION_END = "restarhalf.schedule.DND_END"
        const val ACTION_TOGGLE = "restarhalf.schedule.DND_TOGGLE"
    }
}

/**
 * 开考/结束勿扰边界闹钟，与 [ClassDndReceiver] 完全一致：
 * 接收器不直接「进入/退出」，而是按当前时间对账 ——
 * 闹钟迟到、丢失或考试安排刚被修改时，对账结果都正确。
 */
class ExamDndReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val toggle = intent.action == ACTION_TOGGLE
        if (!toggle && intent.action != ACTION_START && intent.action != ACTION_END) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (toggle) {
                    // 通知 / 超级岛上的「考试勿扰」按钮：与课程一致，点一下立即切换
                    ExamDndController.toggleFromNotification(context)
                } else {
                    ExamDndController.applyCurrentState(context)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_START = "restarhalf.schedule.EXAM_DND_START"
        const val ACTION_END = "restarhalf.schedule.EXAM_DND_END"
        const val ACTION_TOGGLE = "restarhalf.schedule.EXAM_DND_TOGGLE"
    }
}
