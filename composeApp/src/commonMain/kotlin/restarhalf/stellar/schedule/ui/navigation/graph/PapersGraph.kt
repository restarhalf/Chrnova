package restarhalf.stellar.schedule.ui.navigation.graph

import org.koin.compose.viewmodel.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.screens.papers.PapersDetailScreen
import restarhalf.stellar.schedule.ui.screens.papers.PapersListScreen
import restarhalf.stellar.schedule.ui.screens.papers.PapersUploadScreen
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder

/** 资料（Papers）路由：列表、详情、上传。 */
fun NavEntryBuilder.papersEntries(deps: AppNavDeps) {
    val navigator = deps.navigator
    entry<Screen.Papers> {
        val paperVm: PapersViewModel = koinViewModel()
        PapersListScreen(
            vm = paperVm,
            onBack = { navigator.pop() },
            onPaperDetail = { paperId ->
                navigator.push(Screen.PapersDetail(paperId))
            },
            onUploadClick = { navigator.push(Screen.PapersUpload) },
        )
    }
    entry<Screen.PapersDetail> { screen ->
        val paperVm: PapersViewModel = koinViewModel()
        PapersDetailScreen(
            vm = paperVm,
            paperId = screen.paperId,
            onBack = { navigator.pop() },
            onDownload = { url, _ -> deps.openUri(url) },
        )
    }
    entry<Screen.PapersUpload> {
        val paperVm: PapersViewModel = koinViewModel()
        PapersUploadScreen(
            vm = paperVm,
            onBack = { navigator.pop() },
            onResult = { deps.showMessage(it) },
            pdfFilePickerHost = deps.pdfFilePickerHost,
        )
    }
}
