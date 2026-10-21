package restarhalf.stellar.schedule.ui.navigation.graph

import androidx.compose.runtime.Composable
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.papers.PickedAttachment
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel

/**
 * 导航图所需的宿主依赖：平台回调、全局 ViewModel 与少量派生状态。
 * 由 [restarhalf.stellar.schedule.AppContent] 组装，逐个传给各图的 entry 注册函数。
 */
class AppNavDeps(
    val navigator: AppNavigator,
    val appUpdate: AppUpdatePort,
    val bgVm: BackgroundViewModel,
    val announcementVm: AnnouncementViewModel,
    val totalWeeks: Int,
    val pictureSelectorHost: @Composable (
        show: Boolean,
        onDismissRequest: () -> Unit,
        onPicked: (String) -> Unit,
        outputWidthPx: Int?,
        outputHeightPx: Int?,
    ) -> Unit,
    val pdfFilePickerHost: @Composable (
        multiple: Boolean,
        onPicked: (List<PickedAttachment>) -> Unit,
    ) -> Unit,
    val ensureNotificationPermission: (onGranted: () -> Unit) -> Unit,
    val openUri: (String) -> Boolean,
    val showMessage: (String) -> Unit,
    val canSaveAwardPicture: Boolean,
    val saveAwardPicture: suspend (fileName: String, bytes: ByteArray) -> Boolean,
    val saveLog: suspend (fileName: String, content: String) -> String?,
    val canSaveImage: Boolean,
    val saveImageFromUrl: suspend (String) -> Boolean,
    val onLogoutJwxt: () -> Unit,
)
