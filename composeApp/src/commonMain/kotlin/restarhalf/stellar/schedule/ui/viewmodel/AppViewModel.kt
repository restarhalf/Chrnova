package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
import restarhalf.stellar.schedule.mcp.McpRuntime
import restarhalf.stellar.schedule.ui.sync.SyncUiState

class AppViewModel(

    getCampusUseCase: GetCampusUseCase,
    observeCampusUseCase: ObserveCampusUseCase,
    getTotalWeeksUseCase: GetTotalWeeksUseCase,
    observeTotalWeeksUseCase: ObserveTotalWeeksUseCase,
    getTermStartMsUseCase: GetTermStartMsUseCase,
    observeTermStartMsUseCase: ObserveTermStartMsUseCase,
    private val setCampusUseCase: SetCampusUseCase,
    private val setTermStartMsUseCase: SetTermStartMsUseCase,
    private val clearAuth: ClearAuthUseCase,
    private val setTotalWeeksUseCase: SetTotalWeeksUseCase,
    private val fetchExaminations: FetchExaminationsSimpleUseCase,
    private val fetchGrades: FetchGradesSimpleUseCase,
    private val loginUseCase: LoginUseCase,
    private val runSyncUseCase: RunSyncUseCase,
    private val mcpRuntime: McpRuntime,
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
                        viewModelScope.launch {
                            mcpRuntime.emitClientEvent(
                                McpRuntime.ClientEventType.SyncCompleted,
                                buildJsonObject {
                                    put("inserted", it.inserted)
                                    put("campusName", it.campusName)
                                },
                            )
                            mcpRuntime.emitClientEvent(McpRuntime.ClientEventType.TimetableUpdated)
                        }
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
        viewModelScope.launch { mcpRuntime.emitClientEvent(McpRuntime.ClientEventType.LoginExpired) }
    }

    fun onCampusChanged(campus: Campus) {
        setCampusUseCase(campus)
        viewModelScope.launch { mcpRuntime.emitClientEvent(McpRuntime.ClientEventType.CapabilityChanged) }
    }

    fun onTermStartMsChanged(ms: Long) {
        setTermStartMsUseCase(ms)
        viewModelScope.launch { mcpRuntime.emitClientEvent(McpRuntime.ClientEventType.CapabilityChanged) }
    }

    fun onTotalWeeksChanged(weeks: Int) {
        setTotalWeeksUseCase(weeks)
        viewModelScope.launch { mcpRuntime.emitClientEvent(McpRuntime.ClientEventType.CapabilityChanged) }
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
