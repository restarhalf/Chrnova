package restarhalf.stellar.schedule

import androidx.compose.runtime.remember
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
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.EventKit.EKEntityType
import platform.EventKit.EKEventStore
import platform.EventKit.EKAuthorizationStatusAuthorized
import platform.EventKit.EKAuthorizationStatusFullAccess
import platform.EventKit.EKAuthorizationStatusNotDetermined
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
import platform.UIKit.UIViewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.di.appModule
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.papers.PdfFilePickerHost
import restarhalf.stellar.schedule.pictureselector.PictureSelectorHost
import restarhalf.stellar.schedule.ui.theme.rememberAppThemeController
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
fun AppRoot(): UIViewController {
    ensureKoinStarted()
    lateinit var hostViewController: UIViewController
    hostViewController = ComposeUIViewController {
        val settings =
            remember {
                KoinPlatform.getKoin().get<ObservableSettings>(named(SettingsKeys.PREFS_NAME))
            }
        val themeController = rememberAppThemeController(settings)
        MiuixTheme(controller = themeController) {
            AppRoot(
                pictureSelectorHost = { show, onDismissRequest, onPicked, outputWidthPx, outputHeightPx ->
                    PictureSelectorHost(
                        hostViewController = hostViewController,
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
                saveLog = { fileName, content ->
                    saveLogToFile(fileName, content)
                },
                saveCsv = { fileName, content ->
                    saveLogToFile(fileName, content)
                },
                canSaveImage = true,
                saveImage = { _, bytes ->
                    saveAwardPicture(
                        controller = hostViewController,
                        bytes = bytes,
                    )
                },
                exitApp = {
                    UIApplication.sharedApplication.performSelector(
                        NSSelectorFromString("suspend")
                    ) },
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
    AppLogger.init()
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    (setUnhandledExceptionHook { throwable ->
        AppLogger.logFatal("main", throwable)
    })
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

@OptIn(ExperimentalForeignApi::class)
private fun ensureNotificationPermission(
    controller: UIViewController,
    onGranted: () -> Unit,
) {
    val store = EKEventStore()
    val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
    when (status) {
        EKAuthorizationStatusAuthorized,
        EKAuthorizationStatusFullAccess,
        -> runOnMain { onGranted() }

        EKAuthorizationStatusNotDetermined -> {
            store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
                if (granted) {
                    runOnMain { onGranted() }
                }
            }
        }

        else -> runOnMain {
            showNotificationPermissionAlert(controller)
        }
    }
}

private fun showNotificationPermissionAlert(controller: UIViewController) {
    val presenter = resolvePresenter(controller)
    val alert =
        UIAlertController.alertControllerWithTitle(
            title = "日历权限未开启",
            message = "请先在系统设置中允许访问日历，再开启课程或考试提醒。",
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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun saveLogToFile(fileName: String, content: String): String? {
    return runCatching {
        val dirPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).firstOrNull() as? String ?: return@runCatching null
        val dirUrl = NSURL.fileURLWithPath("$dirPath/Chrnova")
        NSFileManager.defaultManager.createDirectoryAtURL(
            dirUrl, withIntermediateDirectories = true, attributes = null, error = null
        )
        val fileUrl = NSURL.fileURLWithPath("$dirPath/Chrnova/$fileName")
        val bytes = content.encodeToByteArray()
        val nsData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        nsData.writeToURL(fileUrl, atomically = true)
        "$dirPath/Chrnova/$fileName"
    }.onFailure {
        AppLogger.log("App", "保存日志文件失败: fileName=$fileName", it)
    }.getOrDefault(null)
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
    val image = UIImage(data = data)
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