package restarhalf.stellar.schedule.data.impl.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.domain.port.ReminderPermissionPort
import restarhalf.stellar.schedule.domain.port.ReminderPermissionState
import restarhalf.stellar.schedule.reminder.IslandNotificationHelper
import restarhalf.stellar.schedule.reminder.LiveNotificationController
import restarhalf.stellar.schedule.reminder.receiver.FocusCapabilities
import restarhalf.stellar.schedule.reminder.receiver.FocusTier
import restarhalf.stellar.schedule.shizuku.ShizukuManager

class ReminderPermissionPortImpl(
    private val context: Context,
    @Suppress("UNUSED_PARAMETER") settings: ObservableSettings,
) : ReminderPermissionPort {

    // 发测试提醒里有 binder 调用与等待，不能压主线程
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun snapshot(): ReminderPermissionState {
        val tier = FocusCapabilities.resolve(context)
        return ReminderPermissionState(
            notificationGranted = notificationsEnabled(),
            exactAlarmGranted = exactAlarmsAllowed(),
            ignoringBatteryOptimizations = ignoringBatteryOptimizations(),
            dndGranted = dndGranted(),
            islandSupported = tier != FocusTier.NONE,
            islandTierSupported = tier == FocusTier.ISLAND,
            shizukuRunning = ShizukuManager.isShizukuRunning(),
            shizukuAuthorized = ShizukuManager.checkSelfPermission(),
            canSendTestNotification = true,
        )
    }

    override fun openBatteryOptimizationSettings(): Boolean =
        tryOpen(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        ) || tryOpen(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    override fun openAppDetailsSettings(): Boolean =
        tryOpen(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        )

    override fun openDndPolicySettings(): Boolean =
        tryOpen(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))

    override fun requestShizukuPermission(onResult: (Boolean) -> Unit) {
        ShizukuManager.requestPermission(onResult)
    }

    override fun sendTestNotification(island: Boolean) {
        scope.launch {
            if (island) {
                IslandNotificationHelper.sendTestIslandNotification(context)
            } else {
                LiveNotificationController.sendTestLiveNotification(context)
            }
        }
    }

    private fun notificationsEnabled(): Boolean =
        context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: false

    private fun exactAlarmsAllowed(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(android.app.AlarmManager::class.java)
                ?.canScheduleExactAlarms() ?: false
        } else {
            true
        }

    private fun ignoringBatteryOptimizations(): Boolean =
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) ?: false

    private fun dndGranted(): Boolean =
        context.getSystemService(NotificationManager::class.java)
            ?.isNotificationPolicyAccessGranted ?: false

    private fun tryOpen(intent: Intent): Boolean =
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
}
