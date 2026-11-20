package restarhalf.stellar.schedule.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.usecase.IsAnyReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleAllRemindersUseCase
import restarhalf.stellar.schedule.reminder.ClassDndController
import restarhalf.stellar.schedule.reminder.CourseDayAlarmScheduler

class BootReceiver : BroadcastReceiver() {

    private class Deps : KoinComponent {
        val isAnyReminderEnabled: IsAnyReminderEnabledUseCase by inject()
        val rescheduleAllReminders: RescheduleAllRemindersUseCase by inject()
        val reminderPrefs: ObservableSettings by inject(named(SettingsKeys.PREFS_NAME))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val deps = Deps()
                // 重启后系统状态已重置，清掉上次的勿扰接管标记，否则本节课/本场考试不会重新进入勿扰
                ClassDndController.dropStaleSessionAfterReboot(context)
                CourseDayAlarmScheduler.refreshNextDay(context, deps.reminderPrefs)
                // 重启后闹钟全部丢失：按当前课表与考试整批重排（新链路，不再走 WorkManager）
                if (deps.isAnyReminderEnabled()) {
                    deps.rescheduleAllReminders()
                }
            } finally {
                result.finish()
            }
        }
    }
}