package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.usecase.BindUnboundDataUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.JwxtLoginUseCase
import restarhalf.stellar.schedule.domain.usecase.RunSyncUseCase
import restarhalf.stellar.schedule.ui.sync.SyncUiState


class AppViewModel(
    private val auth: JwxtAuthPort,
    private val timetable: TimetablePort,
    private val settings: SettingsPort,
    private val fetchExaminations: FetchExaminationsSimpleUseCase,
    private val fetchGrades: FetchGradesSimpleUseCase,
    private val jwxtLoginUseCase: JwxtLoginUseCase,
    private val runSyncUseCase: RunSyncUseCase,
    private val bindUnboundData: BindUnboundDataUseCase,
    ) : ViewModel() {

    init {
        viewModelScope.launch {
            settings.observeLogEnabled().collect { enabled ->
                AppLogger.setEnabled(enabled)
            }
        }
        viewModelScope.launch {
            runCatching { bindUnboundData() }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("AppViewModel", "数据绑定失败", e)
                }
        }
    }

    
    @Immutable
    data class AppUiState(
        val campus: Campus,
        val termStartMs: Long,
        val totalWeeks: Int,
    )

    private val _uiState: StateFlow<AppUiState> =
        combine(
            timetable.observeCampus(),
            timetable.observeTermStartMs(),
            timetable.observeTotalWeeks(),
        ) { campus, termStartMs, totalWeeks ->
            AppUiState(campus = campus, termStartMs = termStartMs, totalWeeks = totalWeeks)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    AppUiState(
                        campus = timetable.getCampus(),
                        termStartMs = timetable.getTermStartMs(),
                        totalWeeks = timetable.getTotalWeeks(),
                    ),
            )

    
    val uiState: StateFlow<AppUiState> = _uiState

    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)

    
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

    
    fun acknowledgeSyncResult() {
        if (_syncUiState.value is SyncUiState.Error) {
            _syncUiState.value = SyncUiState.Idle
        }
    }

    
    suspend fun runSync() {
        _syncUiState.value = SyncUiState.Loading
        _syncUiState.value =
            runCatching { runSyncUseCase() }
                .fold(
                    onSuccess = {
                        SyncUiState.Success(
                            inserted = it.inserted,
                            campusName = it.campusName
                        )
                    },
                    onFailure = { e ->
                        if (e is CancellationException) throw e
                        AppLogger.log("Sync", "同步失败", e)
                        SyncUiState.Error(e.toUserFacingMessage(UserFacingErrorKind.Sync))
                    },
                )
    }

    
    fun logout() {
        auth.clear()
    }

    
    fun onCampusChanged(campus: Campus) {
        timetable.setCampus(campus)
    }

    
    fun onTermStartMsChanged(ms: Long) {
        timetable.setTermStartMs(ms)
    }

    
    fun onTotalWeeksChanged(weeks: Int) {
        timetable.setTotalWeeks(weeks)
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
        jwxtLoginUseCase(
            userNo = userNo,
            password = password,
            captchaData = captchaData,
            codeVal = codeVal,
            p = p
        )
    }
}
