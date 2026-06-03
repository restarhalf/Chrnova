package restarhalf.stellar.schedule.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.ui.components.StatusBannerListScreen
import restarhalf.stellar.schedule.ui.components.screen.grade.GradeDetailsDialog
import restarhalf.stellar.schedule.ui.components.screen.grade.GradeItemCard
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel

@Composable
fun GradeScreen(onLoadGrades: suspend () -> TermGradeReport) {
    val vm: GradeViewModel = koinViewModel()
    val gradeUiState by vm.uiState.collectAsState()
    val screenUi =
        remember(gradeUiState) {
            vm.buildScreenUi(
                gradeUiState.report,
                gradeUiState.loading,
                gradeUiState.error
            )
        }
    val showGradeDetailsDialog = remember { mutableStateOf(false) }
    var selectedGrade by remember { mutableStateOf<GradeCourse?>(null) }

    LaunchedEffect(onLoadGrades) { vm.bindLoader(onLoadGrades) }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(showGradeDetailsDialog.value) {
        if (!showGradeDetailsDialog.value) selectedGrade = null
    }

    StatusBannerListScreen(
        title = "成绩",
        statusText = screenUi.statusText,
        loading = gradeUiState.loading,
        onRefresh = { vm.load() },
        items = screenUi.cards,
        keySelector = { it.idKey },
        popupHost = {
            if (showGradeDetailsDialog.value) {
                selectedGrade?.let {
                    GradeDetailsDialog(
                        show = showGradeDetailsDialog,
                        title = vm.buildGradeTitle(it),
                        summary = vm.buildGradeDetailsSummary(it)
                    )
                }
            }
        },
        itemContent = { card ->
            GradeItemCard(
                card = card,
                onClick = {
                    selectedGrade = card.grade
                    showGradeDetailsDialog.value = true
                }
            )
        }
    )
}
