package restarhalf.stellar.schedule.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.ui.components.StatusBannerListScreen
import restarhalf.stellar.schedule.ui.components.screen.examination.ExamItemCard
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ExaminationScreen(onLoadExaminations: suspend () -> List<Examination>) {
    val vm: ExaminationViewModel = koinViewModel()
    val examUiState by vm.uiState.collectAsState()

    LaunchedEffect(onLoadExaminations) { vm.bindLoader(onLoadExaminations) }
    LaunchedEffect(Unit) { vm.load() }

    var nowMs by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(300_000L.milliseconds)
            nowMs = Clock.System.now().toEpochMilliseconds()
        }
    }

    val screenUi = remember(examUiState, nowMs) {
        vm.buildScreenUi(
            items = examUiState.items,
            loading = examUiState.loading,
            error = examUiState.error,
            nowMs = nowMs,
        )
    }

    StatusBannerListScreen(
        title = "考试安排",
        statusText = screenUi.statusText,
        loading = examUiState.loading,
        onRefresh = { vm.load() },
        items = screenUi.cards,
        keySelector = { it.idKey },
        itemContent = { card -> ExamItemCard(card = card) }
    )
}
