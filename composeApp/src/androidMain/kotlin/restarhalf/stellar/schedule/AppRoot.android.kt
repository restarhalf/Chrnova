package restarhalf.stellar.schedule

import android.Manifest
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.koin.compose.koinInject
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.port.ExactAlarmPermissionPort
import restarhalf.stellar.schedule.papers.PdfFilePickerHost
import restarhalf.stellar.schedule.pictureselector.PictureSelectorHost
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPort
import restarhalf.stellar.schedule.widget.WidgetUpdateCache

@RequiresApi(Build.VERSION_CODES.Q)
fun ComponentActivity.AppRoot(displaySettings: DisplaySettingsPort) {
    setContent {

        val exactAlarmPermission: ExactAlarmPermissionPort = koinInject()

        val systemDark = isSystemInDarkTheme()
        var darkMode by remember {
            mutableStateOf(
                when (displaySettings.getThemeMode()) {
                    DisplaySettingsPort.THEME_MODE_LIGHT -> false
                    DisplaySettingsPort.THEME_MODE_DARK -> true
                    else -> systemDark
                },
            )
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

        AppRoot(
            onEffectiveDarkChange = { darkMode = it },
            pictureSelectorHost = { show, onDismissRequest, onPicked, outputWidthPx, outputHeightPx ->
                val pictureSelectorPort: PictureSelectorPort = koinInject()
                PictureSelectorHost(
                    show = show,
                    onDismissRequest = onDismissRequest,
                    onPicked = onPicked,
                    outputWidthPx = outputWidthPx,
                    outputHeightPx = outputHeightPx,
                    port = pictureSelectorPort,
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    exactAlarmPermission.openExactAlarmSettings()
                ) {
                    exactAlarmPermission.markAsked()
                }
            },
            onRequestDndPolicyPermission = {
                val manager = getSystemService(NotificationManager::class.java)
                if (manager?.isNotificationPolicyAccessGranted != true) {
                    runCatching {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                }
            },
            openUri = { uri ->
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
                    true
                }.getOrDefault(false)
            },
            showMessage = { message ->
                Toast.makeText(this@AppRoot, message, Toast.LENGTH_SHORT).show()
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
                }.getOrDefault(false)
            },
            exitApp = { finishAffinity() },
            // 小组件外观「应用」后：清签名并现场重绘所有已放置的小组件
            onRequestWidgetRefresh = {
                WidgetUpdateCache.clear()
                WidgetUpdateCache.updateInstalledWidgets(this@AppRoot)
            },
        )
    }
}

