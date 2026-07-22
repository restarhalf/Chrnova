package restarhalf.stellar.schedule.data.impl

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.update.ApkDownloadState
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.core.update.checkUpdateFromWorker

class AppUpdatePortImpl(
    private val context: Context,
) : AppUpdatePort {
    private val appContext = context.applicationContext
    private val _apkDownloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    override val apkDownloadState: StateFlow<ApkDownloadState> = _apkDownloadState.asStateFlow()

    override suspend fun check(currentVersionName: String): AppUpdateInfo? =
        checkUpdateFromWorker(currentVersionName)

    override fun startDirectDownload(info: AppUpdateInfo) {
        val intent = Intent(Intent.ACTION_VIEW, info.downloadUrl.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun cancelApkDownload() = Unit

    @RequiresApi(Build.VERSION_CODES.O)
    override fun canRequestInstallPackages(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    @RequiresApi(Build.VERSION_CODES.O)
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
        val file = java.io.File(apkPath)
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
}
