package restarhalf.stellar.schedule

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.pictureselector.PictureSelectorHost
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

fun ComponentActivity.AppRoot(settings: ObservableSettings) {
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
                ensureNotificationPermission = { onGranted ->
                    if (
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
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
                    }.getOrDefault(false)
                },
                showMessage = { message ->
                    Toast.makeText(this@AppRoot, message, Toast.LENGTH_SHORT).show()
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
