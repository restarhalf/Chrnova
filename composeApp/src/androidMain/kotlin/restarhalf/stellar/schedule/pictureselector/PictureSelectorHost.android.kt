package restarhalf.stellar.schedule.pictureselector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.koin.compose.koinInject

@Composable
fun PictureSelectorHost(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onPicked: (String) -> Unit,
) {
    val context = LocalContext.current
    val port: PictureSelectorPort = koinInject()
    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    var hasPermission by remember(show) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
        }
    val outputSize = context.resolveCropOutputSize()

    PictureSelectorSheet(
        show = show,
        hasPermission = hasPermission,
        permissionSummary = "需要读取相册权限才能选择图片",
        onRequestPermission = { permissionLauncher.launch(permission) },
        outputWidthPx = outputSize.widthPx,
        outputHeightPx = outputSize.heightPx,
        onDismissRequest = onDismissRequest,
        onPicked = onPicked,
        port = port,
    )
}

private fun Context.resolveCropOutputSize(): OutputSize {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val bounds = windowManager.currentWindowMetrics.bounds
    return OutputSize(
        widthPx = bounds.width().coerceAtLeast(1),
        heightPx = bounds.height().coerceAtLeast(1),
    )
}

private data class OutputSize(
    val widthPx: Int,
    val heightPx: Int,
)
