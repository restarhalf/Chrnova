package restarhalf.stellar.schedule.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.reminder.EXTRA_REMINDER_SESSION
import restarhalf.stellar.schedule.reminder.IslandNotificationHelper
import restarhalf.stellar.schedule.reminder.LiveNotificationController
import restarhalf.stellar.schedule.reminder.LiveReminderState
import restarhalf.stellar.schedule.reminder.ReminderSession
import restarhalf.stellar.schedule.reminder.ReminderTickScheduler
import restarhalf.stellar.schedule.reminder.sessionFromIntent

/**
 * 上课/开考瞬间触发器（原生实况）：倒计时精确切到「已有课中 / 已上课 / 考试中 / 已开考」。
 * 闹钟时间来自倒计时状态，状态已被别的场次覆盖时 updateActiveCountdown 自行跳过。
 * 课程与考试共用，会话种类由 [EXTRA_REMINDER_SESSION] 携带。
 */
class CourseStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                LiveNotificationController.updateActiveCountdown(context)
                IslandNotificationHelper.reconcileIslandCountdown(context)
                ReminderTickScheduler.scheduleNext(context)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                result.finish()
            }
        }
    }
}

/**
 * 超级岛展开态触发器：倒计时结束后发广播触发展开态弹出（切到「已上课/已开考 / 进行中」）。
 */
class IslandExpandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val session =
            if (intent.hasExtra(EXTRA_REMINDER_SESSION)) {
                sessionFromIntent(intent)
            } else {
                IslandNotificationHelper.sessionForIslandId(notificationId)
            }
        // 闹钟可能因 Doze 严重延迟，到达时岛已是另一场。开始时间对不上就当迟到旧闹钟丢弃。
        val expectedStart = intent.getLongExtra(EXTRA_COURSE_START_MILLIS, -1L)
        val state = LiveReminderState.islandSnapshot(session)
        if (expectedStart > 0L && (state == null || state.startMillis != expectedStart)) return
        if (state == null || state.switched) return

        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: state.courseName
        val classroom = intent.getStringExtra(EXTRA_CLASSROOM) ?: state.classroom
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: state.startTime
        val endTime =
            intent.getStringExtra(EXTRA_END_TIME)?.takeIf { it.isNotEmpty() } ?: state.endTime

        IslandNotificationHelper.onClassStart(
            context = context,
            courseName = courseName,
            classroom = classroom,
            startTime = startTime,
            endTime = endTime,
            notificationId =
                if (notificationId > 0) notificationId else state.notificationId,
            session = session,
        )

        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderTickScheduler.scheduleNext(context)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_CLASSROOM = "classroom"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_COURSE_START_MILLIS = "course_start_millis"
    }
}

/**
 * 超级岛「已上课/已开考」自动收起触发器。
 * 只靠分钟链最坏等一整分钟；进程被杀未重启就永远收不起。精确闹钟到点立即收。
 */
class IslandDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId =
            intent.getIntExtra(
                EXTRA_NOTIFICATION_ID,
                IslandNotificationHelper.ISLAND_NOTIFICATION_ID,
            )
        val session =
            if (intent.hasExtra(EXTRA_REMINDER_SESSION)) {
                sessionFromIntent(intent)
            } else {
                IslandNotificationHelper.sessionForIslandId(notificationId)
            }
        val expectedStart = intent.getLongExtra(EXTRA_COURSE_START_MILLIS, -1L)
        val state = LiveReminderState.islandSnapshot(session)

        if (expectedStart > 0L) {
            // 传了 startMillis：必须与当前 state 一致才执行，否则是迟到的旧闹钟
            if (state == null || state.startMillis != expectedStart) return
        } else {
            // 没传 startMillis 但岛仍活跃：保守不动，避免误收刚发出的新一轮倒计时
            if (state != null) return
        }

        IslandNotificationHelper.cancelIslandState(context, notificationId)
        LiveReminderState.clearIsland(session)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_COURSE_START_MILLIS = "course_start_millis"
    }
}