package restarhalf.stellar.schedule.pictureselector

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun PictureSelectorHost(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onPicked: (String) -> Unit,
    port: PictureSelectorPort,
    outputWidthPx: Int? = null,
    outputHeightPx: Int? = null,
) {
    val context = LocalContext.current
    val permission = remember {
        Manifest.permission.READ_MEDIA_IMAGES
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
    PictureSelectorSheet(
        show = show,
        hasPermission = hasPermission,
        permissionSummary = "需要读取相册权限才能选择图片",
        onRequestPermission = { permissionLauncher.launch(permission) },
        outputWidthPx = outputWidthPx,
        outputHeightPx = outputHeightPx,
        onDismissRequest = onDismissRequest,
        onPicked = onPicked,
        port = port,
    )
}
