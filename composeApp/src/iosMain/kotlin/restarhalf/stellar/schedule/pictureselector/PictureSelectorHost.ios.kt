@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package restarhalf.stellar.schedule.pictureselector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.compose.koinInject
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@Composable
fun PictureSelectorHost(
    hostViewController: UIViewController,
    show: Boolean,
    onDismissRequest: () -> Unit,
    onPicked: (String) -> Unit,
    outputWidthPx: Int? = null,
    outputHeightPx: Int? = null,
) {
    if (!show) return

    val port: PictureSelectorPort = koinInject()
    val defaultOutputSize = rememberIosCropOutputSize(hostViewController)
    val scope = rememberCoroutineScope()
    var permissionState by remember(show) { mutableStateOf(currentPhotoPermissionState()) }

    LaunchedEffect(show) {
        if (show) {
            permissionState = currentPhotoPermissionState()
        }
    }

    PictureSelectorSheet(
        show = true,
        hasPermission = permissionState.granted,
        permissionSummary = permissionState.summary,
        onRequestPermission = {
            scope.launch {
                permissionState = requestPhotoPermissionState()
            }
        },
        outputWidthPx = outputWidthPx ?: defaultOutputSize.widthPx,
        outputHeightPx = outputHeightPx ?: defaultOutputSize.heightPx,
        onDismissRequest = onDismissRequest,
        onPicked = onPicked,
        port = port,
    )
}

private fun rememberIosCropOutputSize(hostViewController: UIViewController): OutputSize {
    val scale = UIScreen.mainScreen.scale
    val viewBounds = hostViewController.view.bounds
    val screenBounds = UIScreen.mainScreen.bounds
    val widthPt =
        CGRectGetWidth(viewBounds)
            .takeIf { it > 1.0 }
            ?: CGRectGetWidth(screenBounds)
    val heightPt =
        CGRectGetHeight(viewBounds)
            .takeIf { it > 1.0 }
            ?: CGRectGetHeight(screenBounds)
    return OutputSize(
        widthPx = (widthPt * scale).roundToInt().coerceAtLeast(1),
        heightPx = (heightPt * scale).roundToInt().coerceAtLeast(1),
    )
}

private fun currentPhotoPermissionState(): PhotoPermissionState =
    PHPhotoLibrary.authorizationStatus().toPermissionState()

private suspend fun requestPhotoPermissionState(): PhotoPermissionState =
    suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.requestAuthorization { status ->
            if (continuation.isActive) {
                continuation.resume(status.toPermissionState())
            }
        }
    }

private fun PHAuthorizationStatus.toPermissionState(): PhotoPermissionState =
    when (this) {
        PHAuthorizationStatusAuthorized,
        PHAuthorizationStatusLimited,
            -> PhotoPermissionState(true, "")

        PHAuthorizationStatusDenied,
        PHAuthorizationStatusRestricted,
            -> PhotoPermissionState(false, "请在系统设置里允许访问照片库")

        else -> PhotoPermissionState(false, "需要读取照片库来选择背景图片")
    }

private data class PhotoPermissionState(
    val granted: Boolean,
    val summary: String,
)

private data class OutputSize(
    val widthPx: Int,
    val heightPx: Int,
)

internal fun normalizeUIImage(image: UIImage): UIImage? {
    val size = image.size
    val width = size.useContents { width }
    val height = size.useContents { height }
    UIGraphicsBeginImageContextWithOptions(size, false, image.scale)
    return try {
        image.drawInRect(CGRectMake(0.0, 0.0, width, height))
        UIGraphicsGetImageFromCurrentImageContext()
    } finally {
        UIGraphicsEndImageContext()
    }
}

internal fun writeTempJpeg(image: UIImage, fileNamePrefix: String): String? {
    val normalized = normalizeUIImage(image) ?: return null
    val data = UIImageJPEGRepresentation(normalized, 0.95) ?: return null
    val path = "${
        platform.Foundation.NSTemporaryDirectory().trimEnd('/')
    }/${fileNamePrefix}_${platform.Foundation.NSUUID().UUIDString}.jpg"
    return if (data.writeToFile(path, true)) path else null
}

internal fun localFileUrl(path: String): NSURL = NSURL.fileURLWithPath(path)
