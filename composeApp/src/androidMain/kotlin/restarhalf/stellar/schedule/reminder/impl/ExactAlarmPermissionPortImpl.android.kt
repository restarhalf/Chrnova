package restarhalf.stellar.schedule.reminder.impl

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.ExactAlarmPermissionPort

class ExactAlarmPermissionPortImpl(
    private val context: Context,
) : ExactAlarmPermissionPort {

    override fun isExactAlarmAllowed(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) canScheduleExactAlarms() else true

    override fun openExactAlarmSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        if (isExactAlarmAllowed()) return false

        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        }.onFailure {
            AppLogger.log("App", "申请精确闹钟权限失败", it)
        }.getOrDefault(false)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun canScheduleExactAlarms(): Boolean =
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: false
}
