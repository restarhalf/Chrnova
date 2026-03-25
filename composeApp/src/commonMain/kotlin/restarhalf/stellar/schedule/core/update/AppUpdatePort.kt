package restarhalf.stellar.schedule.core.update

import kotlinx.coroutines.flow.StateFlow

interface AppUpdatePort {
    val apkDownloadState: StateFlow<ApkDownloadState>

    suspend fun check(currentVersionName: String): AppUpdateInfo?

    fun startDirectDownload(info: AppUpdateInfo)

    fun cancelApkDownload()

    fun canRequestInstallPackages(): Boolean

    fun openUnknownSourcesSettings()

    fun launchInstaller(apkPath: String): Boolean

    fun saveWxpayToPictures(): Boolean

    fun joinQqGroup(key: String?): Boolean

    fun openWeChatScanDirect(): Boolean
}
