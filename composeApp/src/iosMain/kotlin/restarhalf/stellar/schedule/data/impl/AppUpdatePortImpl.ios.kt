package restarhalf.stellar.schedule.data.impl

import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.core.update.buildQqGroupIosUrl
import restarhalf.stellar.schedule.core.update.buildQqGroupWebUrl

class AppUpdatePortImpl : AppUpdatePort {

    override fun startDirectDownload(info: AppUpdateInfo) {
        openUri(info.downloadUrl) || openUri(info.releasePageUrl)
    }

    override fun saveWxpayToPictures(): Boolean = false

    override fun joinQqGroup(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        return openUri(buildQqGroupIosUrl()) || openUri(buildQqGroupWebUrl(key))
    }

    override fun openWeChatScanDirect(): Boolean = false

    private fun openUri(uriString: String): Boolean {
        val normalized = uriString.trim().filterNot { it.isWhitespace() }
        val url = NSURL.URLWithString(normalized) ?: return false
        val scheme = (url.scheme ?: "").lowercase()
        if (scheme == "http" || scheme == "https") {
            val opened = openUrlWithOptionsOnMain(url)
            if (opened || scheme != "http") return opened
            val httpsUrl =
                NSURL.URLWithString("https://" + normalized.removePrefix("http://")) ?: return false
            return openUrlWithOptionsOnMain(httpsUrl)
        }
        return canOpenUrlOnMain(url) && openUrlWithOptionsOnMain(url)
    }

    private fun canOpenUrlOnMain(url: NSURL): Boolean =
        runOnMainSyncBoolean { UIApplication.sharedApplication.canOpenURL(url) }

    private fun openUrlWithOptionsOnMain(url: NSURL): Boolean =
        runOnMainSyncBoolean {
            UIApplication.sharedApplication.openURL(
                url = url,
                options = emptyMap<Any?, Any>(),
                completionHandler = null,
            )
            true
        }

    private fun runOnMainSyncBoolean(block: () -> Boolean): Boolean {
        if (NSThread.isMainThread) return block()
        var result = false
        dispatch_sync(dispatch_get_main_queue()) {
            result = block()
        }
        return result
    }
}
