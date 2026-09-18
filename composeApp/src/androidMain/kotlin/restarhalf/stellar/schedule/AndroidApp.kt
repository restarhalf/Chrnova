package restarhalf.stellar.schedule

import android.annotation.SuppressLint
import android.app.Application
import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.initialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import restarhalf.stellar.schedule.data.local.mmkv.MmkvLegacyMigrator
import restarhalf.stellar.schedule.di.appModule
import restarhalf.stellar.schedule.reminder.NotificationChannels
import restarhalf.stellar.schedule.reminder.ReminderWorkScheduler
import restarhalf.stellar.schedule.widget.ScreenStateReceiver

class AndroidApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val screenStateReceiver = ScreenStateReceiver()

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        if (!isMainProcess()) return

        MmkvLegacyMigrator.migrateAll(this)

        registerReceiver(screenStateReceiver, ScreenStateReceiver.intentFilter())

        startKoin {
            androidContext(this@AndroidApp)
            modules(appModule)
        }

        NotificationChannels.ensureAll(this)
        appScope.launch { ReminderWorkScheduler.enqueueDaily(this@AndroidApp) }
    }

    private fun isMainProcess(): Boolean = packageName == getProcessName()
}

