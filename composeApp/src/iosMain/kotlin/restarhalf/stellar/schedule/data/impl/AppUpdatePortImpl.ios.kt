package restarhalf.stellar.schedule.data.impl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync
import restarhalf.stellar.schedule.core.update.ApkDownloadState
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.core.update.GiteeLatestReleaseResponse
import restarhalf.stellar.schedule.core.update.IOS_RELEASE_IPA_FILE_NAME
import restarhalf.stellar.schedule.core.update.buildGiteeLatestReleaseApi
import restarhalf.stellar.schedule.core.update.buildGiteeReleaseAssetUrl
import restarhalf.stellar.schedule.core.update.buildGiteeReleasePageUrl
import restarhalf.stellar.schedule.core.update.buildQqGroupIosUrl
import restarhalf.stellar.schedule.core.update.buildQqGroupWebUrl
import restarhalf.stellar.schedule.core.update.isNewerVersion
import restarhalf.stellar.schedule.core.update.resolvedLatestVersion

class AppUpdatePortImpl : AppUpdatePort {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient()
    private val _apkDownloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)

    override val apkDownloadState: StateFlow<ApkDownloadState> = _apkDownloadState

    override suspend fun check(currentVersionName: String): AppUpdateInfo? {
        val response = client.get(buildGiteeLatestReleaseApi())
        if (!response.status.isSuccess()) {
            throw IllegalStateException("检查更新失败（HTTP ${response.status.value}）")
        }

        val latest = json.decodeFromString<GiteeLatestReleaseResponse>(response.body())
        val latestVersion = resolvedLatestVersion(latest)
        if (latestVersion.isBlank()) {
            throw IllegalStateException("检查更新失败：未获取到版本号")
        }
        if (!isNewerVersion(latestVersion, currentVersionName)) return null

        val releasePageUrl =
            latest.htmlUrl?.takeIf { it.isNotBlank() } ?: buildGiteeReleasePageUrl(latestVersion)
        val downloadUrl = buildGiteeReleaseAssetUrl(latestVersion, IOS_RELEASE_IPA_FILE_NAME)
        return AppUpdateInfo(
            latestVersion = latestVersion,
            releasePageUrl = releasePageUrl,
            downloadUrl = downloadUrl,
            changelog = latest.body,
        )
    }

    override fun startDirectDownload(info: AppUpdateInfo) {
        openUri(info.downloadUrl) || openUri(info.releasePageUrl)
    }

    override fun cancelApkDownload() = Unit

    override fun canRequestInstallPackages(): Boolean = true

    override fun openUnknownSourcesSettings() = Unit

    override fun launchInstaller(apkPath: String): Boolean = openUri(apkPath)

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
