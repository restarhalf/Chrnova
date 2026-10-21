package restarhalf.stellar.schedule.ui.navigation.graph

import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.screens.announcement.AnnouncementDetailScreen
import restarhalf.stellar.schedule.ui.screens.announcement.AnnouncementImageViewerScreen
import restarhalf.stellar.schedule.ui.screens.announcement.AnnouncementListScreen
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder

/** 公告与图片查看路由。 */
fun NavEntryBuilder.announcementEntries(deps: AppNavDeps) {
    val navigator = deps.navigator
    entry<Screen.AnnouncementList> {
        AnnouncementListScreen(
            vm = deps.announcementVm,
            onBack = { navigator.pop() },
            onAnnouncementClick = { announcementId ->
                navigator.push(Screen.AnnouncementDetail(announcementId))
            },
            onAdOpenUrl = { deps.openUri(it) },
        )
    }
    entry<Screen.AnnouncementDetail> { screen ->
        AnnouncementDetailScreen(
            vm = deps.announcementVm,
            announcementId = screen.announcementId,
            onBack = { navigator.pop() },
            onImageClick = { url ->
                navigator.push(Screen.AnnouncementImageViewer(url))
            },
        )
    }
    entry<Screen.AnnouncementImageViewer> { screen ->
        AnnouncementImageViewerScreen(
            url = screen.url,
            alt = null,
            canSaveImage = deps.canSaveImage,
            saveImage = deps.saveImageFromUrl,
            showMessage = deps.showMessage,
            onBack = { navigator.pop() },
        )
    }
}
