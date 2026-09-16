package restarhalf.stellar.schedule.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.domain.usecase.IsAnyReminderEnabledUseCase
import restarhalf.stellar.schedule.reminder.ReminderWorkScheduler

class BootReceiver : BroadcastReceiver() {

    private class Deps : KoinComponent {
        val isAnyReminderEnabled: IsAnyReminderEnabledUseCase by inject()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val deps = Deps()
                if (deps.isAnyReminderEnabled()) {
                    ReminderWorkScheduler.enqueueNow(context)
                }
            } finally {
                result.finish()
            }
        }
    }
}
