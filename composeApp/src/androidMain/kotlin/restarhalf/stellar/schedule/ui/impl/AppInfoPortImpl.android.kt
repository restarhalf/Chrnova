package restarhalf.stellar.schedule.ui.impl

import android.content.Context
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
                .getOrNull()
                ?.versionName
                .orEmpty()
                .ifBlank { "dev" }
}
