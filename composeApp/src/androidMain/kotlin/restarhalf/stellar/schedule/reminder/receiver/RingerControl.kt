package restarhalf.stellar.schedule.reminder.receiver

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import restarhalf.stellar.schedule.core.log.AppLogger

internal object RingerControl {

    fun canChange(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)
            ?.isNotificationPolicyAccessGranted == true

    fun requestAccess(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } catch (e: Exception) {
            AppLogger.log("Reminder", "打开勿扰权限设置失败", e)
        }
    }
}
