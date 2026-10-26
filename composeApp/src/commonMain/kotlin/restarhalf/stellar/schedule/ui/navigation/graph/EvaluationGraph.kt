package restarhalf.stellar.schedule.ui.navigation.graph

import org.koin.compose.viewmodel.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.screens.evaluation.EvaluationCourseScreen
import restarhalf.stellar.schedule.ui.screens.evaluation.EvaluationDetailScreen
import restarhalf.stellar.schedule.ui.screens.evaluation.EvaluationListScreen
import restarhalf.stellar.schedule.ui.screens.evaluation.EvaluationSubmitScreen
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder

fun NavEntryBuilder.evaluationEntries(deps: AppNavDeps) {
    val navigator = deps.navigator
    entry<Screen.Evaluation> {
        val evalVm: CourseEvaluationViewModel = koinViewModel()
        EvaluationListScreen(
            vm = evalVm,
            onBack = { navigator.pop() },
            onCourseClick = { courseName, teacher ->
                navigator.push(Screen.EvaluationCourse(courseName, teacher))
            },
            onEvaluationDetail = { evaluationId ->
                navigator.push(Screen.EvaluationDetail(evaluationId))
            },
            onSubmitClick = { navigator.push(Screen.EvaluationSubmit()) },
        )
    }
    entry<Screen.EvaluationCourse> { screen ->
        val evalVm: CourseEvaluationViewModel = koinViewModel()
        EvaluationCourseScreen(
            vm = evalVm,
            courseName = screen.courseName,
            teacher = screen.teacher,
            onBack = { navigator.pop() },
            onEvaluationDetail = { evaluationId ->
                navigator.push(Screen.EvaluationDetail(evaluationId))
            },
        )
    }
    entry<Screen.EvaluationDetail> { screen ->
        val evalVm: CourseEvaluationViewModel = koinViewModel()
        EvaluationDetailScreen(
            vm = evalVm,
            evaluationId = screen.evaluationId,
            onBack = { navigator.pop() },
            onEditEvaluation = { evaluationId ->
                navigator.push(Screen.EvaluationSubmit(evaluationId))
            },
        )
    }
    entry<Screen.EvaluationSubmit> { screen ->
        val evalVm: CourseEvaluationViewModel = koinViewModel()
        EvaluationSubmitScreen(
            vm = evalVm,
            onBack = { navigator.pop() },
            onSubmitted = { navigator.pop() },
            evaluationId = screen.evaluationId,
        )
    }
}
