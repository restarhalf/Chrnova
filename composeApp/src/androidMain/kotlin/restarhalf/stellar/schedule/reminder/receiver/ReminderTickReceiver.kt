package restarhalf.stellar.schedule.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.reminder.ClassDndController
import restarhalf.stellar.schedule.reminder.ExamDndController
import restarhalf.stellar.schedule.reminder.IslandNotificationHelper
import restarhalf.stellar.schedule.reminder.LiveNotificationController
import restarhalf.stellar.schedule.reminder.ReminderTickScheduler

/**
 * 分钟级对账链的接收器，照 Nexio 的 WidgetRefreshReceiver：
 * 跨日重排 → 课前/考前兜底补发 → 上课/考试勿扰对账，最后无条件把下一次刷新排上。
 */
class ReminderTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderTickScheduler.ACTION_TICK) return

        val result = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderTickScheduler.rescheduleIfDayChanged(appContext)
                ReminderTickScheduler.catchUpPendingPreClass(appContext)
                // 实况倒计时/进行中进度与岛状态对账（兜底切换闹钟丢失/Doze 延迟）
                LiveNotificationController.updateActiveCountdown(appContext)
                IslandNotificationHelper.reconcileIslandCountdown(appContext)
                ClassDndController.applyCurrentState(appContext)
                ExamDndController.applyCurrentState(appContext)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                // 放在 finally：即使上面任一步抛异常，链条也不能断
                ReminderTickScheduler.scheduleNext(appContext)
                result.finish()
            }
        }
    }
}
