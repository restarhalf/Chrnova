package restarhalf.stellar.schedule.ui.impl

import android.content.Context
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.ui.port.AppInfoPort

class AppInfoPortImpl(
    private val context: Context,
) : AppInfoPort {

    override val appName: String
        get() = context.applicationInfo.loadLabel(context.packageManager).toString()
            .ifBlank { "Chrnova" }

    override val versionName: String
        get() =
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
                .onFailure {
                    AppLogger.log("AppInfo", "获取版本信息失败", it)
                }
                .getOrNull()
                ?.versionName
                .orEmpty()
                .ifBlank { "dev" }
}
