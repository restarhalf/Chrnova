package restarhalf.stellar.schedule.ui.impl

import android.content.Context
import restarhalf.stellar.schedule.ui.port.AppInfoPort

class AppInfoPortImpl(
    private val context: Context,
) : AppInfoPort {

    override val appName: String
        get() = context.applicationInfo.loadLabel(context.packageManager).toString()
            .ifBlank { "\u5927\u6c11\u8bfe\u7a0b\u8868" }

    override val versionName: String
        get() =
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
                .getOrNull()
                ?.versionName
                .orEmpty()
                .ifBlank { "dev" }
}
