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

/**
 * 应用主ViewModel
 * 
 * 管理应用的核心状态，包括校区、学期开始时间、总周数等。
 * 负责处理同步、登录、登出等全局操作。
 */
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
) : ViewModel() {
    /**
     * 应用UI状态数据类
     * 
     * @param campus 当前校区
     * @param termStartMs 学期开始时间戳（毫秒）
     * @param totalWeeks 学期总周数
     */
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

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<AppUiState> = _uiState

    /**
     * 执行教务系统同步
     * 
     * @param updateState 状态更新回调，用于通知UI同步进度
     */
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

    /** 用户登出，清除认证信息 */
    fun logout() {
        clearAuth()
    }

    /** 校区变更回调 */
    fun onCampusChanged(campus: Campus) {
        setCampusUseCase(campus)
    }

    /** 学期开始时间变更回调 */
    fun onTermStartMsChanged(ms: Long) {
        setTermStartMsUseCase(ms)
    }

    /** 总周数变更回调 */
    fun onTotalWeeksChanged(weeks: Int) {
        setTotalWeeksUseCase(weeks)
    }

    /**
     * 获取考试安排
     * 
     * @param semester 学期ID，空字符串表示当前学期
     * @param nameOrNumber 课程名称或编号筛选
     * @return 考试安排列表
     */
    suspend fun fetchExaminationArrangements(
        semester: String = "",
        nameOrNumber: String = ""
    ): List<Examination> {
        return fetchExaminations(semester = semester, nameOrNumber = nameOrNumber)
    }

    /**
     * 获取成绩报告
     * 
     * @param semester 学期ID，空字符串表示当前学期
     * @return 学期成绩报告
     */
    suspend fun fetchGradeReport(semester: String = ""): TermGradeReport {
        return fetchGrades(semester = semester)
    }

    /**
     * 用户登录
     * 
     * @param userNo 学号
     * @param password 密码
     * @param captchaData 验证码数据（Base64编码）
     * @param codeVal 用户输入的验证码
     * @param p 加密参数（可选）
     */
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
