package restarhalf.stellar.schedule.ui.navigation.graph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.screens.AboutScreen
import restarhalf.stellar.schedule.ui.screens.ChangeBackgroundScreen
import restarhalf.stellar.schedule.ui.screens.CourseEditScreen
import restarhalf.stellar.schedule.ui.screens.ExamEditScreen
import restarhalf.stellar.schedule.ui.screens.JwxtLoginScreen
import restarhalf.stellar.schedule.ui.screens.LogScreen
import restarhalf.stellar.schedule.ui.screens.ProfileScreen
import restarhalf.stellar.schedule.ui.viewmodel.AboutViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExamEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.JwxtLoginViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PersonalInfoViewModel
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder

/** 其余页面：背景、关于、课程/考试编辑、日志、个人资料、教务登录。 */
fun NavEntryBuilder.miscEntries(deps: AppNavDeps) {
    val navigator = deps.navigator

    entry<Screen.ChangeBackground> {
        ChangeBackgroundScreen(
            vm = deps.bgVm,
            onBack = { navigator.pop() },
            pictureSelectorHost = deps.pictureSelectorHost,
        )
    }
    entry<Screen.About> {
        val aboutVm: AboutViewModel = koinViewModel()
        AboutScreen(
            vm = aboutVm,
            onBack = { navigator.pop() },
            showMessage = deps.showMessage,
            canSaveAwardPicture = deps.canSaveAwardPicture,
            onSaveAwardPicture = deps.saveAwardPicture,
            onIconTap = { navigator.push(Screen.Log) },
            onHandleEvent = { event ->
                when (event) {
                    is AboutViewModel.AboutUiEvent.OpenUri -> {
                        if (!deps.openUri(event.uri)) {
                            deps.showMessage("无法打开链接")
                        }
                    }

                    is AboutViewModel.AboutUiEvent.JoinQqGroup -> {
                        if (!deps.appUpdate.joinQqGroup(key = event.key)) {
                            deps.showMessage("请检查是否安装了 QQ")
                        }
                    }

                    AboutViewModel.AboutUiEvent.WxPayAwardRequested -> {
                        val saved = deps.appUpdate.saveWxpayToPictures()
                        deps.showMessage(
                            if (saved) {
                                "赞赏码已保存到相册，即将跳转到微信扫一扫"
                            } else {
                                "保存赞赏码失败，请重试"
                            },
                        )
                        if (!deps.appUpdate.openWeChatScanDirect()) {
                            deps.showMessage("微信启动失败，请检查是否安装微信")
                        }
                    }
                }
            },
            onStartDownload = { info -> deps.appUpdate.startDirectDownload(info) },
        )
    }
    entry<Screen.ClassEdit> { screen ->
        val isEdit = remember(screen.courseId) { screen.courseId != null }
        val courseEditVm: CourseEditViewModel = koinViewModel()
        CourseEditScreen(
            vm = courseEditVm,
            onBack = { navigator.pop() },
            isEdit = isEdit,
            onEditChanged = { },
            courseId = screen.courseId,
            initialDayOfWeek = screen.dayOfWeek,
            initialStartSection = screen.startSection,
            initialSelectedWeek = screen.selectedWeek,
            totalWeeks = deps.totalWeeks,
            showMessage = deps.showMessage,
        )
    }
    entry<Screen.ExamEdit> { screen ->
        val isEdit = remember(screen.examinationId) { screen.examinationId != null }
        val examEditVm: ExamEditViewModel = koinViewModel()
        ExamEditScreen(
            vm = examEditVm,
            onBack = { navigator.pop() },
            isEdit = isEdit,
            onEditChanged = { },
            examinationId = screen.examinationId,
            showMessage = deps.showMessage,
        )
    }
    entry<Screen.Log> {
        val scope = rememberCoroutineScope()
        LogScreen(
            onBack = { navigator.pop() },
            onExport = { fileName, content ->
                scope.launch {
                    val path = deps.saveLog(fileName, content)
                    deps.showMessage(if (path != null) "已保存: $path" else "保存失败")
                }
            },
        )
    }
    entry<Screen.Profile> {
        val settingsVm: SettingsViewModel = koinViewModel()
        val settingsUiState by settingsVm.uiState.collectAsStateWithLifecycle()
        val peVm: PEViewModel = koinViewModel()
        val peUiState by peVm.uiState.collectAsStateWithLifecycle()
        val personalInfoViewModel: PersonalInfoViewModel = koinViewModel()
        ProfileScreen(
            peAuthProfile = peUiState.authProfile,
            jwxtAuthProfile = settingsUiState.profile,
            onBack = { navigator.pop() },
            onLogoutJW = deps.onLogoutJwxt,
            onLogoutPE = { peVm.logout() },
            pictureSelectorHost = deps.pictureSelectorHost,
            personalInfoViewModel = personalInfoViewModel,
        )
    }
    entry<Screen.JwxtLogin> {
        val jwLoginVm: JwxtLoginViewModel = koinViewModel()
        JwxtLoginScreen(
            vm = jwLoginVm,
            onBack = { navigator.pop() },
            onLoginSuccess = { navigator.pop() },
            inWel = false,
            next = {}
        )
    }
}
