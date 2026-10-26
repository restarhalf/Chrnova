package restarhalf.stellar.schedule.ui.navigation.graph

import org.koin.compose.viewmodel.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.screens.pe.PEDetailScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEFreeApplyDetailScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEFreeApplyEditScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEFreeApplyScreen
import restarhalf.stellar.schedule.ui.screens.pe.PELoginScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEQRCodeScreen
import restarhalf.stellar.schedule.ui.screens.pe.PESubjectHistoryScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEAppointmentScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEAppointmentWebScreen
import restarhalf.stellar.schedule.ui.viewmodel.PEAppointmentViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PEFreeApplyViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PELoginViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder

fun NavEntryBuilder.peEntries(deps: AppNavDeps) {
    val navigator = deps.navigator
    entry<Screen.PEDetail> { screen ->
        val peVm: PEViewModel = koinViewModel()
        PEDetailScreen(
            vm = peVm,
            schoolYear = screen.schoolYear,
            onSubjectClick = { subject ->
                navigator.push(
                    Screen.PESubjectHistory(
                        schoolYear = screen.schoolYear,
                        subjectId = subject.subjectId,
                        subjectName = subject.subName,
                        unit = subject.unit,
                        currentResult = subject.result,
                    )
                )
            },
            onBack = { navigator.pop() }
        )
    }
    entry<Screen.PESubjectHistory> { screen ->
        val peVm: PEViewModel = koinViewModel()
        PESubjectHistoryScreen(
            vm = peVm,
            schoolYear = screen.schoolYear,
            subjectId = screen.subjectId,
            subjectName = screen.subjectName,
            unit = screen.unit,
            currentResult = screen.currentResult,
            onBack = { navigator.pop() }
        )
    }
    entry<Screen.PEQRCode> {
        val settingsVm: SettingsViewModel = koinViewModel()
        val settingsUiState by settingsVm.uiState.collectAsStateWithLifecycle()
        val peVm: PEViewModel = koinViewModel()
        PEQRCodeScreen(
            vm = peVm,
            jwxtAuthProfile = settingsUiState.profile,
            onBack = { navigator.pop() },
        )
    }
    entry<Screen.PEAppointment> {
        val peAppointmentVm: PEAppointmentViewModel = koinViewModel()
        PEAppointmentScreen(
            vm = peAppointmentVm,
            onLogin = { navigator.push(Screen.PELogin) },
            onOpenWeb = { navigator.push(Screen.PEAppointmentWeb) },
            onBack = { navigator.pop() },
        )
    }
    entry<Screen.PEAppointmentWeb> {
        PEAppointmentWebScreen(
            onBack = { navigator.pop() },
        )
    }
    entry<Screen.PEFreeApply> {
        val peFreeVm: PEFreeApplyViewModel = koinViewModel()
        PEFreeApplyScreen(
            vm = peFreeVm,
            onLogin = { navigator.push(Screen.PELogin) },
            onApply = {
                peFreeVm.prepareApplyForm()
                navigator.push(Screen.PEFreeApplyEdit)
            },
            onDetail = { applyId ->
                navigator.push(Screen.PEFreeApplyDetail(applyId))
            },
            onBack = { navigator.pop() },
        )
    }
    entry<Screen.PEFreeApplyEdit> {
        val peFreeVm: PEFreeApplyViewModel = koinViewModel()
        PEFreeApplyEditScreen(
            vm = peFreeVm,
            onBack = { navigator.pop() },
            onSubmitted = { navigator.pop() },
            onImageClick = { url ->
                navigator.push(Screen.AnnouncementImageViewer(url))
            },
            pdfFilePickerHost = deps.pdfFilePickerHost,
        )
    }
    entry<Screen.PEFreeApplyDetail> { screen ->
        val peFreeVm: PEFreeApplyViewModel = koinViewModel()
        PEFreeApplyDetailScreen(
            vm = peFreeVm,
            applyId = screen.applyId,
            onImageClick = { url ->
                navigator.push(Screen.AnnouncementImageViewer(url))
            },
            onBack = { navigator.pop() },
        )
    }
    entry<Screen.PELogin> {
        val peLoginVm: PELoginViewModel = koinViewModel()
        PELoginScreen(
            vm = peLoginVm,
            onBack = { navigator.pop() },
            onLoginSuccess = { navigator.pop() },
            inWel = false,
            next = {}
        )
    }
}
