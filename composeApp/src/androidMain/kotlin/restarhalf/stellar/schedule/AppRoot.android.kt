package restarhalf.stellar.schedule

import android.Manifest
import android.app.AlarmManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
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
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.papers.PdfFilePickerHost
import restarhalf.stellar.schedule.pictureselector.PictureSelectorHost

@RequiresApi(Build.VERSION_CODES.Q)
fun ComponentActivity.AppRoot(displaySettings: DisplaySettingsPort) {
    AppLogger.init(filesDir.absolutePath + "/logs")
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching { AppLogger.logFatal(thread.name, throwable) }
        defaultHandler?.uncaughtException(thread, throwable)
    }
    setContent {

        var themeMode by remember { mutableIntStateOf(displaySettings.getThemeMode()) }
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
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkMode },
                navigationBarStyle =
                    SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
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

        // SCHEDULE_EXACT_ALARM 走的是「特殊权限」，不是运行时权限，
        // 必须弹系统设置页让用户开。不开的话 setExact 会被降级成
        // setAndAllowWhileIdle，闹钟被塞进最长 1 小时的模糊窗口，
        // 表现就是「排上了但到点不响」。
        val exactAlarmLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

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
            pdfFilePickerHost = { multiple, onPicked ->
                PdfFilePickerHost(multiple = multiple, onPicked = onPicked)
            },
            ensureNotificationPermission = { onGranted ->

                val perms = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                val permsArray = perms.toTypedArray()
                val allGranted = permsArray.isEmpty() || permsArray.all {
                    ContextCompat.checkSelfPermission(this@AppRoot, it) == PackageManager.PERMISSION_GRANTED
                }
                if (allGranted) {
                    onGranted()
                } else {
                    pendingNotificationGrant = onGranted
                    notificationPermissionLauncher.launch(permsArray)
                }
            },
            ensureExactAlarmPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestExactAlarmIfNeeded(exactAlarmLauncher)
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

/**
 * 没开精确闹钟权限就跳系统设置页。
 *
 * 跳过或没开成也不阻断流程——闹钟仍然会排，只是走 setAndAllowWhileIdle，
 * 到点时间可能被推迟（实测有 40 多分钟的窗口）。
 */
@RequiresApi(Build.VERSION_CODES.S)
private fun ComponentActivity.requestExactAlarmIfNeeded(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
) {
    val alarmManager = getSystemService(AlarmManager::class.java) ?: return
    if (alarmManager.canScheduleExactAlarms()) return

    runCatching {
        launcher.launch(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }.onFailure {
        AppLogger.log("App", "申请精确闹钟权限失败", it)
    }
}
