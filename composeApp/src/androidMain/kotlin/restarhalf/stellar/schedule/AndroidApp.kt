package restarhalf.stellar.schedule

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import restarhalf.stellar.schedule.di.appModule
import restarhalf.stellar.schedule.reminder.NotificationChannels
import restarhalf.stellar.schedule.reminder.ReminderWorkScheduler
import restarhalf.stellar.schedule.widget.ScreenStateReceiver

class AndroidApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val screenStateReceiver = ScreenStateReceiver()

    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) return

        registerReceiver(screenStateReceiver, ScreenStateReceiver.intentFilter())

        startKoin {
            androidContext(this@AndroidApp)
            modules(appModule)
        }


        NotificationChannels.ensureAll(this)

        appScope.launch { ReminderWorkScheduler.enqueueDaily(this@AndroidApp) }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun isMainProcess(): Boolean = packageName == getProcessName()
}
