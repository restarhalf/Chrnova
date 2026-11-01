package restarhalf.stellar.schedule.data.impl

import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_sync
import platform.darwin.dispatch_time
import restarhalf.stellar.schedule.core.log.AppLogger
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

    private fun openUrlWithOptionsOnMain(url: NSURL): Boolean {
        if (NSThread.isMainThread) {
            if (!UIApplication.sharedApplication.canOpenURL(url)) return false
            UIApplication.sharedApplication.openURL(
                url = url,
                options = emptyMap<Any?, Any>(),
            ) { success ->
                if (!success) AppLogger.log("AppUpdate", "打开链接失败")
            }
            return true
        }
        val semaphore = dispatch_semaphore_create(0)
        var opened = false
        dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.openURL(
                url = url,
                options = emptyMap<Any?, Any>(),
            ) { success ->
                opened = success
                dispatch_semaphore_signal(semaphore)
            }
        }
        dispatch_semaphore_wait(semaphore, dispatch_time(DISPATCH_TIME_NOW, 2_000_000_000L))
        return opened
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
