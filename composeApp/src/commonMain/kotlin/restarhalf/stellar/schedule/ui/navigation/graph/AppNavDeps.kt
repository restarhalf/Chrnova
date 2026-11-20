package restarhalf.stellar.schedule.ui.navigation.graph

import androidx.compose.runtime.Composable
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.papers.PickedAttachment
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel

class AppNavDeps(
    val navigator: AppNavigator,
    val appUpdate: AppUpdatePort,
    val bgVm: BackgroundViewModel,
    val announcementVm: AnnouncementViewModel,
    val appVm: AppViewModel,
    val runSync: suspend () -> Unit,
    val ensureExactAlarmPermission: () -> Unit,
    val onRequestDndPolicyPermission: () -> Unit,
    val saveCsv: suspend (fileName: String, content: String) -> String?,
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
    val canSaveImage: Boolean,
    val saveImageFromUrl: suspend (String) -> Boolean,
    val onLogoutJwxt: () -> Unit,
    /** 「应用」小组件外观后：清签名缓存并刷新全部已安装小组件（iOS 无 AppWidget，空实现） */
    val onRequestWidgetRefresh: () -> Unit,
)
