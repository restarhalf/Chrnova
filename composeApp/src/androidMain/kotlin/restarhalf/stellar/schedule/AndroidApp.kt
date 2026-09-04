package restarhalf.stellar.schedule

import android.annotation.SuppressLint
import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import restarhalf.stellar.schedule.di.appModule
import restarhalf.stellar.schedule.widget.ScreenStateReceiver

class AndroidApp : Application() {
    private val screenStateReceiver = ScreenStateReceiver()

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) return

        registerReceiver(screenStateReceiver, ScreenStateReceiver.intentFilter())

        startKoin {
            androidContext(this@AndroidApp)
            modules(appModule)
        }
    }

    private fun isMainProcess(): Boolean = packageName == getProcessName()
}
