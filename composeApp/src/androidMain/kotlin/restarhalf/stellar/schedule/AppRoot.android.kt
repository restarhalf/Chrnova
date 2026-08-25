package restarhalf.stellar.schedule

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.papers.PdfFilePickerHost
import restarhalf.stellar.schedule.pictureselector.PictureSelectorHost

@RequiresApi(Build.VERSION_CODES.Q)
fun ComponentActivity.AppRoot(settings: ObservableSettings) {
    AppLogger.init(filesDir.absolutePath + "/logs")
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching { AppLogger.logFatal(thread.name, throwable) }
        defaultHandler?.uncaughtException(thread, throwable)
    }
    setContent {
        // 镜像主题模式（真源在 common AppRoot 的 AppState，经 onThemeModeChange 同步），
        // 仅用于 edge-to-edge 系统栏样式；初值直接读 settings 避免冷启动闪错色
        var themeMode by remember { mutableIntStateOf(settings.getInt(SettingsKeys.THEME_MODE, 0)) }
        val darkMode =
            when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

        DisposableEffect(darkMode) {
            enableEdgeToEdge(
                statusBarStyle =
                    SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkMode },
                navigationBarStyle =
                    SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkMode },
            )
            window.isNavigationBarContrastEnforced = false
            onDispose {}
        }

        var pendingNotificationGrant by remember { mutableStateOf<(() -> Unit)?>(null) }
        val notificationPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                val onGranted = pendingNotificationGrant
                pendingNotificationGrant = null
                val granted = result.values.all { it }
                if (granted) {
                    onGranted?.invoke()
                }
            }

        AppRoot(
            onThemeModeChange = { themeMode = it },
            pictureSelectorHost = { show, onDismissRequest, onPicked, outputWidthPx, outputHeightPx ->
                PictureSelectorHost(
                    show = show,
                    onDismissRequest = onDismissRequest,
                    onPicked = onPicked,
                    outputWidthPx = outputWidthPx,
                    outputHeightPx = outputHeightPx,
                )
            },
            pdfFilePickerHost = { onPicked ->
                PdfFilePickerHost(onPicked = onPicked)
            },
            ensureNotificationPermission = { onGranted ->
                // 课表/考试提醒需要日历权限，后台抢课 Foreground Service 需要通知权限（Android 13+）
                val perms = mutableListOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                val permsArray = perms.toTypedArray()
                val allGranted = permsArray.all {
                    ContextCompat.checkSelfPermission(this@AppRoot, it) == PackageManager.PERMISSION_GRANTED
                }
                if (allGranted) {
                    onGranted()
                } else {
                    pendingNotificationGrant = onGranted
                    notificationPermissionLauncher.launch(permsArray)
                }
            },
            openUri = { uri ->
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
                    true
                }.onFailure {
                    AppLogger.log("App", "打开URI失败", it)
                }.getOrDefault(false)
            },
            showMessage = { message ->
                Toast.makeText(this@AppRoot, message, Toast.LENGTH_SHORT).show()
            },
            saveLog = { fileName, content ->
                runCatching {
                    val collection =
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Chrnova")
                    }
                    val uri = contentResolver.insert(collection, values)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { os ->
                            os.write(content.toByteArray(Charsets.UTF_8))
                        }
                    }
                    if (uri != null) "${Environment.DIRECTORY_DOCUMENTS}/Chrnova/$fileName" else null
                }.onFailure {
                    AppLogger.log("App", "保存日志文件失败: fileName=$fileName", it)
                }.getOrDefault(null)
            },
            saveCsv = { fileName, content ->
                runCatching {
                    val mimeType = when {
                        fileName.endsWith(".ics", ignoreCase = true) -> "text/calendar"
                        fileName.endsWith(".csv", ignoreCase = true) -> "text/csv"
                        else -> "text/plain"
                    }
                    val collection =
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Chrnova")
                    }
                    val uri = contentResolver.insert(collection, values)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { os ->
                            os.write(content.toByteArray(Charsets.UTF_8))
                        }
                    }
                    if (uri != null) "${Environment.DIRECTORY_DOCUMENTS}/Chrnova/$fileName" else null
                }.onFailure {
                    AppLogger.log("App", "保存CSV文件失败: fileName=$fileName", it)
                }.getOrDefault(null)
            },
            canSaveImage = true,
            saveImage = { fileName, bytes ->
                runCatching {
                    val collection =
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/Chrnova",
                        )
                    }
                    val uri = contentResolver.insert(collection, values) ?: return@runCatching false
                    contentResolver.openOutputStream(uri)?.use { os -> os.write(bytes) }
                        ?: return@runCatching false
                    true
                }.onFailure {
                    AppLogger.log("App", "保存图片失败: fileName=$fileName", it)
                }.getOrDefault(false)
            },
            exitApp = { finishAffinity() },
        )
    }
}
