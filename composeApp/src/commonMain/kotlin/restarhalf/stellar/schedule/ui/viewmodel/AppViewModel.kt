package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
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
import restarhalf.stellar.schedule.domain.usecase.LoginUseCase
import restarhalf.stellar.schedule.domain.usecase.RunSyncUseCase
import restarhalf.stellar.schedule.domain.usecase.SetCampusUseCase
import restarhalf.stellar.schedule.domain.usecase.SetTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.SetTotalWeeksUseCase
import restarhalf.stellar.schedule.ui.sync.SyncUiState

class AppViewModel(
    private val clearAuth: ClearAuthUseCase,
    private val getCampusUseCase: GetCampusUseCase,
    private val setCampusUseCase: SetCampusUseCase,
    private val getTermStartMsUseCase: GetTermStartMsUseCase,
    private val setTermStartMsUseCase: SetTermStartMsUseCase,
    private val getTotalWeeksUseCase: GetTotalWeeksUseCase,
    private val setTotalWeeksUseCase: SetTotalWeeksUseCase,
    private val fetchExaminations: FetchExaminationsSimpleUseCase,
    private val fetchGrades: FetchGradesSimpleUseCase,
    private val loginUseCase: LoginUseCase,
    private val runSyncUseCase: RunSyncUseCase,
) : ViewModel() {
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
