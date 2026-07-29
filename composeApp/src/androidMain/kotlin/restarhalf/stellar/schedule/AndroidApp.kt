package restarhalf.stellar.schedule

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import restarhalf.stellar.schedule.di.appModule
import restarhalf.stellar.schedule.widget.ScreenStateReceiver

class AndroidApp : Application() {
    private val screenStateReceiver = ScreenStateReceiver()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) return

        registerReceiver(screenStateReceiver, ScreenStateReceiver.intentFilter())

        startKoin {
            androidContext(this@AndroidApp)
            modules(appModule)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun isMainProcess(): Boolean = packageName == getProcessName()
}
