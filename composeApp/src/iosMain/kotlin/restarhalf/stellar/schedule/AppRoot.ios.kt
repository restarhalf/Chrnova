package restarhalf.stellar.schedule

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.ComposeUIViewController
import com.russhwolf.settings.ObservableSettings
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import platform.Foundation.NSData
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIViewController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync
import platform.posix.memcpy
import restarhalf.stellar.schedule.di.appModule
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.pictureselector.PictureSelectorHost
import restarhalf.stellar.schedule.ui.theme.rememberAppThemeController
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.coroutines.resume

fun AppRoot(): UIViewController {
    ensureKoinStarted()
    lateinit var hostViewController: UIViewController
    hostViewController = ComposeUIViewController {
        val settings =
            remember {
                KoinPlatform.getKoin().get<ObservableSettings>(named(SettingsKeys.PREFS_NAME))
            }
        val appIcon = remember { loadIosAppIcon() }
        val themeController = rememberAppThemeController(settings)
        MiuixTheme(controller = themeController) {
            AppRoot(
                appIcon = appIcon,
                pictureSelectorHost = { show, onDismissRequest, onPicked ->
                    PictureSelectorHost(
                        hostViewController = hostViewController,
                        show = show,
                        onDismissRequest = onDismissRequest,
                        onPicked = onPicked,
                    )
                },
                openUri = ::openUri,
                ensureNotificationPermission = { onGranted ->
                    ensureNotificationPermission(
                        controller = hostViewController,
                        onGranted = onGranted,
                    )
                },
                showMessage = { message ->
                    showNativeMessage(hostViewController, message)
                },
                canSaveAwardPicture = true,
                saveAwardPicture = { _, bytes ->
                    saveAwardPicture(
                        controller = hostViewController,
                        bytes = bytes,
                    )
                },
            )
        }
    }
    return hostViewController
}

private fun ensureKoinStarted() {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        modules(appModule)
    }
}

private fun openUri(uriString: String): Boolean {
    val normalized = sanitizeUri(uriString)
    val url = NSURL.URLWithString(normalized) ?: return false
    val scheme = (url.scheme ?: "").lowercase()
    if (scheme == "http" || scheme == "https") {
        val opened = openUrlWithOptionsOnMain(url)
        if (opened || scheme != "http") return opened
        val httpsUrl =
            NSURL.URLWithString("https://" + normalized.removePrefix("http://")) ?: return false
        return openUrlWithOptionsOnMain(httpsUrl)
    }
    return canOpenUrlOnMain(url) && openUrlWithOptionsOnMain(url)
}

private fun sanitizeUri(raw: String): String = raw.trim().filterNot { it.isWhitespace() }

private fun canOpenUrlOnMain(url: NSURL): Boolean =
    runOnMainSyncBoolean { UIApplication.sharedApplication.canOpenURL(url) }

private fun openUrlWithOptionsOnMain(url: NSURL): Boolean =
    runOnMainSyncBoolean {
        UIApplication.sharedApplication.openURL(
            url = url,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
        true
    }

private fun ensureNotificationPermission(
    controller: UIViewController,
    onGranted: () -> Unit,
) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.getNotificationSettingsWithCompletionHandler { settings ->
        val status = settings?.authorizationStatus
        when (status) {
            UNAuthorizationStatusAuthorized,
            UNAuthorizationStatusProvisional,
            UNAuthorizationStatusEphemeral,
                -> runOnMain { onGranted() }

            UNAuthorizationStatusNotDetermined -> {
                center.requestAuthorizationWithOptions(
                    options =
                        UNAuthorizationOptionAlert or
                                UNAuthorizationOptionSound or
                                UNAuthorizationOptionBadge,
                    completionHandler = { granted, _ ->
                        if (granted) {
                            runOnMain { onGranted() }
                        }
                    },
                )
            }

            else -> runOnMain {
                showNotificationPermissionAlert(controller)
            }
        }
    }
}

private fun showNotificationPermissionAlert(controller: UIViewController) {
    val presenter = resolvePresenter(controller)
    val alert =
        UIAlertController.alertControllerWithTitle(
            title = "通知权限未开启",
            message = "请先在系统设置中允许通知，再开启课程或考试提醒。",
            preferredStyle = UIAlertControllerStyleAlert,
        )
    alert.addAction(
        UIAlertAction.actionWithTitle(
            title = "取消",
            style = UIAlertActionStyleCancel,
            handler = null,
        ),
    )
    alert.addAction(
        UIAlertAction.actionWithTitle(
            title = "去设置",
            style = UIAlertActionStyleDefault,
            handler = {
                openAppSettings()
            },
        ),
    )
    presenter.presentViewController(alert, animated = true, completion = null)
}

private fun openAppSettings(): Boolean {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return false
    return openUrlWithOptionsOnMain(url)
}

private fun showNativeMessage(controller: UIViewController, message: String) {
    if (message.isBlank()) return
    runOnMain {
        val presenter = resolvePresenter(controller)
        val alert =
            UIAlertController.alertControllerWithTitle(
                title = null,
                message = message,
                preferredStyle = UIAlertControllerStyleAlert,
            )
        alert.addAction(
            UIAlertAction.actionWithTitle(
                title = "确定",
                style = UIAlertActionStyleDefault,
                handler = null,
            ),
        )
        presenter.presentViewController(alert, animated = true, completion = null)
    }
}

private fun resolvePresenter(controller: UIViewController): UIViewController {
    var current = controller
    while (true) {
        val presented = current.presentedViewController ?: return current
        if (presented is UIAlertController) return current
        current = presented
    }
}

private fun runOnMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue(), block)
}

private fun runOnMainSyncBoolean(block: () -> Boolean): Boolean {
    if (NSThread.isMainThread) return block()
    var result = false
    dispatch_sync(dispatch_get_main_queue()) {
        result = block()
    }
    return result
}

private fun loadIosAppIcon(): ImageBitmap? {
    val candidates =
        listOf(
            "AppIcon60x60",
            "AppIcon76x76",
            "AppIcon83.5x83.5",
            "AppIcon1024x1024",
            "AppIcon",
        )
    for (name in candidates) {
        val image = UIImage.imageNamed(name) ?: continue
        val png = UIImagePNGRepresentation(image) ?: continue
        val bitmap = png.toComposeImageBitmapOrNull()
        if (bitmap != null) return bitmap
    }
    return null
}

private suspend fun saveAwardPicture(
    controller: UIViewController,
    bytes: ByteArray,
): Boolean =
    withContext(Dispatchers.Main) {
        val image = bytes.toUIImageForPhotoSave() ?: return@withContext false
        val status = ensurePhotoLibraryPermission()
        if (status != PHAuthorizationStatusAuthorized && status != PHAuthorizationStatusLimited) {
            runOnMain {
                showPhotoLibraryPermissionAlert(controller)
            }
            return@withContext false
        }
        return@withContext saveImageToPhotoLibrary(image)
    }

private fun showPhotoLibraryPermissionAlert(controller: UIViewController) {
    val presenter = resolvePresenter(controller)
    val alert =
        UIAlertController.alertControllerWithTitle(
            title = "照片权限未开启",
            message = "请先在系统设置中允许访问照片，再保存赞赏码。",
            preferredStyle = UIAlertControllerStyleAlert,
        )
    alert.addAction(
        UIAlertAction.actionWithTitle(
            title = "取消",
            style = UIAlertActionStyleCancel,
            handler = null,
        ),
    )
    alert.addAction(
        UIAlertAction.actionWithTitle(
            title = "去设置",
            style = UIAlertActionStyleDefault,
            handler = {
                openAppSettings()
            },
        ),
    )
    presenter.presentViewController(alert, animated = true, completion = null)
}

private suspend fun ensurePhotoLibraryPermission(): PHAuthorizationStatus {
    return when (val current = PHPhotoLibrary.authorizationStatus()) {
        PHAuthorizationStatusAuthorized,
        PHAuthorizationStatusLimited,
            -> current

        PHAuthorizationStatusDenied,
        PHAuthorizationStatusRestricted,
            -> current

        else -> requestPhotoLibraryPermission()
    }
}

private suspend fun requestPhotoLibraryPermission(): PHAuthorizationStatus =
    suspendCancellableCoroutine { continuation ->
        runOnMain {
            PHPhotoLibrary.requestAuthorization { status ->
                if (continuation.isActive) {
                    continuation.resume(status)
                }
            }
        }
    }

private suspend fun saveImageToPhotoLibrary(image: UIImage): Boolean =
    suspendCancellableCoroutine { continuation ->
        runOnMain {
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                {
                    PHAssetChangeRequest.creationRequestForAssetFromImage(image)
                },
                { success, _ ->
                    if (continuation.isActive) {
                        continuation.resume(success)
                    }
                },
            )
        }
    }

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toUIImageForPhotoSave(): UIImage? {
    if (isEmpty()) return null
    val data =
        usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = size.toULong(),
            )
        }
    val image = UIImage(data = data) ?: return null
    return image.normalizeForPhotoSave() ?: image
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.normalizeForPhotoSave(): UIImage? {
    val imageSize = size
    val width = imageSize.useContents { width }
    val height = imageSize.useContents { height }
    if (width <= 0.0 || height <= 0.0) return null
    UIGraphicsBeginImageContextWithOptions(imageSize, false, scale)
    return try {
        drawInRect(platform.CoreGraphics.CGRectMake(0.0, 0.0, width, height))
        UIGraphicsGetImageFromCurrentImageContext()
    } finally {
        UIGraphicsEndImageContext()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toComposeImageBitmapOrNull(): ImageBitmap? {
    val size = length.toInt()
    if (size <= 0) return null
    val source = bytes ?: return null
    val bytesArray =
        ByteArray(size).also { buffer ->
            buffer.usePinned { pinned ->
                memcpy(pinned.addressOf(0), source, length)
            }
        }
    return runCatching { Image.makeFromEncoded(bytesArray).toComposeImageBitmap() }.getOrNull()
}
