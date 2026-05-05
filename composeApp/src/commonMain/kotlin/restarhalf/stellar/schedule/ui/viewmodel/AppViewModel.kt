package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.usecase.ClearAuthUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.GetCampusUseCase
import restarhalf.stellar.schedule.domain.usecase.GetTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.GetTotalWeeksUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveCampusUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveTotalWeeksUseCase
import restarhalf.stellar.schedule.domain.usecase.LoginUseCase
import restarhalf.stellar.schedule.domain.usecase.RunSyncUseCase
import restarhalf.stellar.schedule.domain.usecase.SetCampusUseCase
import restarhalf.stellar.schedule.domain.usecase.SetTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.SetTotalWeeksUseCase
import restarhalf.stellar.schedule.ui.sync.SyncUiState

class AppViewModel(
    private val clearAuth: ClearAuthUseCase,
    private val getCampusUseCase: GetCampusUseCase,
    private val observeCampusUseCase: ObserveCampusUseCase,
    private val setCampusUseCase: SetCampusUseCase,
    private val getTermStartMsUseCase: GetTermStartMsUseCase,
    private val observeTermStartMsUseCase: ObserveTermStartMsUseCase,
    private val setTermStartMsUseCase: SetTermStartMsUseCase,
    private val getTotalWeeksUseCase: GetTotalWeeksUseCase,
    private val observeTotalWeeksUseCase: ObserveTotalWeeksUseCase,
    private val setTotalWeeksUseCase: SetTotalWeeksUseCase,
    private val fetchExaminations: FetchExaminationsSimpleUseCase,
    private val fetchGrades: FetchGradesSimpleUseCase,
    private val loginUseCase: LoginUseCase,
    private val runSyncUseCase: RunSyncUseCase,
) : ViewModel() {
    data class AppUiState(
        val campus: Campus,
        val termStartMs: Long,
        val totalWeeks: Int,
    )

    private val _uiState: StateFlow<AppUiState> =
        combine(
            observeCampusUseCase(),
            observeTermStartMsUseCase(),
            observeTotalWeeksUseCase(),
        ) { campus, termStartMs, totalWeeks ->
            AppUiState(campus = campus, termStartMs = termStartMs, totalWeeks = totalWeeks)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    AppUiState(
                        campus = getCampusUseCase(),
                        termStartMs = getTermStartMsUseCase(),
                        totalWeeks = getTotalWeeksUseCase(),
                    ),
            )

    val uiState: StateFlow<AppUiState> = _uiState
    suspend fun runSync(updateState: (SyncUiState) -> Unit) {
        updateState(SyncUiState.Loading)
        val uiState =
            runCatching { runSyncUseCase() }
                .fold(
                    onSuccess = {
                        SyncUiState.Success(
                            inserted = it.inserted,
                            campusName = it.campusName
                        )
                    },
                    onFailure = {
                        SyncUiState.Error(it.toUserFacingMessage(UserFacingErrorKind.Sync))
                    },
                )

        updateState(uiState)
    }

    fun logout() {
        clearAuth()
    }

    fun getCampus(): Campus = getCampusUseCase()

    fun setCampus(campus: Campus) {
        setCampusUseCase(campus)
    }

    fun getTermStartMs(): Long = getTermStartMsUseCase()

    fun setTermStartMs(ms: Long) {
        setTermStartMsUseCase(ms)
    }

    fun getTotalWeeks(): Int = getTotalWeeksUseCase()

    fun setTotalWeeks(weeks: Int) {
        setTotalWeeksUseCase(weeks)
    }

    suspend fun fetchExaminationArrangements(
        semester: String = "",
        nameOrNumber: String = ""
    ): List<Examination> {
        return fetchExaminations(semester = semester, nameOrNumber = nameOrNumber)
    }

    suspend fun fetchGradeReport(semester: String = ""): TermGradeReport {
        return fetchGrades(semester = semester)
    }

    suspend fun login(
        userNo: String,
        password: String,
        captchaData: String = "",
        codeVal: String = "",
        p: String? = null
    ) {
        loginUseCase(
            userNo = userNo,
            password = password,
            captchaData = captchaData,
            codeVal = codeVal,
            p = p
        )
    }
}
