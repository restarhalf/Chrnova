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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.papers.PdfFilePickerHost
import restarhalf.stellar.schedule.pictureselector.PictureSelectorHost
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@RequiresApi(Build.VERSION_CODES.Q)
fun ComponentActivity.AppRoot(settings: ObservableSettings) {
    AppLogger.init(filesDir.absolutePath + "/logs")
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching { AppLogger.logFatal(thread.name, throwable) }
        defaultHandler?.uncaughtException(thread, throwable)
    }
    setContent {
        val themeMode = rememberThemeMode(settings)
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

        val themeController =
            remember(themeMode) {
                ThemeController(
                    colorSchemeMode =
                        when (themeMode) {
                            1 -> ColorSchemeMode.Light
                            2 -> ColorSchemeMode.Dark
                            else -> ColorSchemeMode.System
                        }
                )
            }

        val appIcon = rememberAppIcon()
        var pendingNotificationGrant by remember { mutableStateOf<(() -> Unit)?>(null) }
        val notificationPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                val onGranted = pendingNotificationGrant
                pendingNotificationGrant = null
                if (granted) {
                    onGranted?.invoke()
                }
            }

        MiuixTheme(controller = themeController) {
            AppRoot(
                appIcon = appIcon,
                pictureSelectorHost = { show, onDismissRequest, onPicked ->
                    PictureSelectorHost(
                        show = show,
                        onDismissRequest = onDismissRequest,
                        onPicked = onPicked,
                    )
                },
                pdfFilePickerHost = { onPicked ->
                    PdfFilePickerHost(onPicked = onPicked)
                },
                ensureNotificationPermission = { onGranted ->
                    if (
                        ContextCompat.checkSelfPermission(
                            this@AppRoot,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        onGranted()
                    } else {
                        pendingNotificationGrant = onGranted
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                exitApp = { finishAffinity() },
            )
        }
    }
}

@Composable
private fun ComponentActivity.rememberAppIcon(): ImageBitmap? =
    remember(this) {
        runCatching {
            packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
        }.onFailure {
            AppLogger.log("App", "获取应用图标失败", it)
        }.getOrNull()
    }

@Composable
private fun rememberThemeMode(settings: ObservableSettings): Int {
    var themeMode by remember(settings) {
        mutableIntStateOf(settings.getInt(SettingsKeys.THEME_MODE, 0))
    }

    DisposableEffect(settings) {
        val listener = settings.addIntListener(SettingsKeys.THEME_MODE, 0) { newValue ->
            themeMode = newValue
        }
        onDispose { listener.deactivate() }
    }

    return themeMode
}
