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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.data.local.mmkv.MmkvLegacyMigrator
import restarhalf.stellar.schedule.data.platform.AppFiles
import restarhalf.stellar.schedule.data.platform.FileAppFile
import restarhalf.stellar.schedule.data.wallpaper.AndroidWallpaperBackend
import restarhalf.stellar.schedule.data.wallpaper.WallpaperStore
import restarhalf.stellar.schedule.di.appModule
import restarhalf.stellar.schedule.domain.usecase.RescheduleAllRemindersUseCase
import restarhalf.stellar.schedule.reminder.NotificationChannels
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

        AppFiles.init(FileAppFile(filesDir))
        val wallpaperBackend = AndroidWallpaperBackend(this)
        WallpaperStore.backend = wallpaperBackend
        val wallpaperPreload =
            java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "chrnova-wallpaper-preload")
            }.submit(
                java.util.concurrent.Callable {
                    runCatching { wallpaperBackend.loadNow() }.getOrNull()
                },
            )
        WallpaperStore.synchronousLoader = {
            runCatching { wallpaperPreload.get(3, java.util.concurrent.TimeUnit.SECONDS) }.getOrNull()
        }

        registerReceiver(screenStateReceiver, ScreenStateReceiver.intentFilter())

        startKoin {
            androidContext(this@AndroidApp)
            modules(appModule)
        }

        NotificationChannels.ensureAll(this)
        restarhalf.stellar.schedule.shizuku.ShizukuManager.init(this)
        // 每次冷启动按当前课表与考试整批重排：取消旧闹钟 → 重排，幂等自愈，
        // 分钟级对账链（ReminderTickScheduler）接管后续跨日与兜底，不再依赖 WorkManager。
        appScope.launch {
            runCatching { StartupDeps().rescheduleAllReminders() }
        }
    }

    private class StartupDeps : KoinComponent {
        val rescheduleAllReminders: RescheduleAllRemindersUseCase by inject()
    }

    private fun isMainProcess(): Boolean = packageName == getProcessName()
}
