package restarhalf.stellar.schedule.data.impl

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.update.ANDROID_RELEASE_APK_FILE_NAME
import restarhalf.stellar.schedule.core.update.ApkDownloadState
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.core.update.GithubLatestReleaseResponse
import restarhalf.stellar.schedule.core.update.buildGithubLatestReleaseApi
import restarhalf.stellar.schedule.core.update.buildGithubReleaseAssetUrl
import restarhalf.stellar.schedule.core.update.buildGithubReleasePageUrl
import restarhalf.stellar.schedule.core.update.isNewerVersion
import restarhalf.stellar.schedule.core.update.resolvedLatestVersion
import java.io.File
import java.io.FileOutputStream

class AppUpdatePortImpl(
    private val context: Context,
) : AppUpdatePort {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _apkDownloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    override val apkDownloadState: StateFlow<ApkDownloadState> = _apkDownloadState.asStateFlow()

    private var activeDownloadJob: Job? = null
    private var activeDownloadFile: File? = null

    override suspend fun check(currentVersionName: String): AppUpdateInfo? =
        withContext(Dispatchers.IO) {
            val response = client.get(buildGithubLatestReleaseApi())
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Check update failed (HTTP ${response.status.value})")
            }

            val payload: String = response.body()
            val latest = json.decodeFromString(GithubLatestReleaseResponse.serializer(), payload)
            val latestVersion = resolvedLatestVersion(latest)
            if (latestVersion.isBlank()) {
                throw IllegalStateException("Latest version is empty")
            }
            if (!isNewerVersion(latestVersion, currentVersionName)) {
                return@withContext null
            }

            val releasePageUrl = latest.htmlUrl?.takeIf { it.isNotBlank() }
                ?: buildGithubReleasePageUrl(latestVersion)
            val downloadUrl =
                buildGithubReleaseAssetUrl(latestVersion, ANDROID_RELEASE_APK_FILE_NAME)
            AppUpdateInfo(
                latestVersion = latestVersion,
                releasePageUrl = releasePageUrl,
                downloadUrl = downloadUrl,
                changelog = latest.body,
            )
        }

    override fun startDirectDownload(info: AppUpdateInfo) {
        cancelApkDownload()

        val downloadDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir == null) {
            _apkDownloadState.value = ApkDownloadState.Error("Download directory unavailable")
            return
        }
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        runCatching {
            downloadDir
                .listFiles()
                ?.filter { it.isFile && it.name.startsWith("schedule-") && it.name.endsWith(".apk") }
                ?.forEach { it.delete() }
        }.onFailure {
            AppLogger.log("Update", "清理旧APK文件失败", it)
        }

        val target =
            File(downloadDir, "schedule-${info.latestVersion}-${System.currentTimeMillis()}.apk")
        activeDownloadFile = target
        _apkDownloadState.value = ApkDownloadState.Downloading(0f, 0L, -1L, target.absolutePath)

        activeDownloadJob = scope.launch {
            val result = runCatching {
                client.prepareGet(info.downloadUrl).execute { response ->
                    if (!response.status.isSuccess()) {
                        error("Download failed (HTTP ${response.status.value})")
                    }

                    val total = response.contentLength() ?: -1L
                    var downloaded = 0L
                    val channel = response.bodyAsChannel()

                    FileOutputStream(target).use { output ->
                        while (!channel.isClosedForRead) {
                            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                            while (!packet.exhausted()) {
                                val bytes = packet.readByteArray()
                                output.write(bytes)
                                downloaded += bytes.size
                                val progress =
                                    if (total > 0L) (downloaded.toDouble() / total.toDouble()).toFloat() * 100f else 0f
                                _apkDownloadState.value =
                                    ApkDownloadState.Downloading(
                                        progress = progress,
                                        downloadedBytes = downloaded,
                                        totalBytes = total,
                                        filePath = target.absolutePath,
                                    )
                            }
                        }
                    }
                    target
                }
            }

            activeDownloadJob = null
            activeDownloadFile = null

            result
                .onSuccess { file ->
                    _apkDownloadState.value = ApkDownloadState.Completed(file.absolutePath)
                }
                .onFailure { error ->
                    AppLogger.log("Update", "下载APK失败", error)
                    runCatching { target.delete() }
                        .onFailure { AppLogger.log("Update", "清理下载文件失败", it) }
                    _apkDownloadState.value =
                        ApkDownloadState.Error(
                            error.toUserFacingMessage(UserFacingErrorKind.DownloadUpdate)
                        )
                }
        }
    }

    override fun cancelApkDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        activeDownloadFile?.let { runCatching { it.delete() }
            .onFailure { AppLogger.log("Update", "取消下载清理文件失败", it) }
        }
        activeDownloadFile = null
        _apkDownloadState.value = ApkDownloadState.Idle
    }

    override fun canRequestInstallPackages(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    override fun openUnknownSourcesSettings() {
        val intent =
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri()
            )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun launchInstaller(apkPath: String): Boolean {
        val file = File(apkPath)
        if (!file.exists()) return false
        val apkUri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
        return true
    }

    override fun saveWxpayToPictures(): Boolean {
        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "wxpay.webp")
                put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/DaMinSchedule"
                )
            }

        val uri =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                context.resources.openRawResource(R.raw.wxpay)
                    .use { input -> input.copyTo(output) }
            } ?: error("openOutputStream returned null")
            true
        }.onFailure {
            AppLogger.log("Update", "保存微信支付图片失败", it)
        }.getOrElse {
            runCatching { resolver.delete(uri, null, null) }
                .onFailure { AppLogger.log("Update", "清理微信支付图片URI失败", it) }
            false
        }
    }

    override fun joinQqGroup(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                "mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key".toUri(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.onFailure {
            AppLogger.log("Update", "加入QQ群失败", it)
        }.getOrDefault(false)
    }

    override fun openWeChatScanDirect(): Boolean {
        return runCatching {
            val intent =
                Intent().apply {
                    component = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
                    putExtra("LauncherUI.From.Scaner.Shortcut", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = "android.intent.action.VIEW"
                }
            context.startActivity(intent)
            true
        }.onFailure {
            AppLogger.log("Update", "打开微信扫一扫失败", it)
        }.getOrDefault(false)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        val client = HttpClient()
    }
}
