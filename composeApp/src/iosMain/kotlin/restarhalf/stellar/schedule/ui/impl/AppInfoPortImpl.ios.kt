package restarhalf.stellar.schedule.ui.impl

import platform.Foundation.NSBundle
import restarhalf.stellar.schedule.ui.port.AppInfoPort

class AppInfoPortImpl : AppInfoPort {
    private val bundle: NSBundle = NSBundle.mainBundle

    override val appName: String =
        (bundle.objectForInfoDictionaryKey("CFBundleDisplayName") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (bundle.objectForInfoDictionaryKey("CFBundleName") as? String)
                ?.takeIf { it.isNotBlank() }
            ?: "Chrnova"

    override val versionName: String =
        (bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)
                ?.takeIf { it.isNotBlank() }
            ?: "dev"
}
