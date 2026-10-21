package restarhalf.stellar.schedule.ui.navigation.graph

import org.koin.compose.viewmodel.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.screens.ElectiveCreditScreen
import restarhalf.stellar.schedule.ui.screens.courseselection.CourseEnrollScreen
import restarhalf.stellar.schedule.ui.screens.courseselection.CourseSelectionScreen
import restarhalf.stellar.schedule.ui.viewmodel.CourseEnrollViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ElectiveCreditViewModel
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder

/** 选课纵切路由：选课、补选、学分统计。 */
fun NavEntryBuilder.courseSelectionEntries(deps: AppNavDeps) {
    val navigator = deps.navigator
    entry<Screen.ElectiveCredit> {
        val electiveCreditVm: ElectiveCreditViewModel = koinViewModel()
        ElectiveCreditScreen(
            vm = electiveCreditVm,
            onBack = { navigator.pop() },
        )
    }
    entry<Screen.CourseEnroll> {
        val courseEnrollVm: CourseEnrollViewModel = koinViewModel()
        CourseEnrollScreen(
            vm = courseEnrollVm,
            onBack = { navigator.pop() },
        )
    }
    entry<Screen.CourseSelection> {
        val courseSelectionVm: CourseSelectionViewModel = koinViewModel()
        CourseSelectionScreen(
            vm = courseSelectionVm,
            onBack = { navigator.pop() },
            ensureNotificationPermission = deps.ensureNotificationPermission,
        )
    }
}
