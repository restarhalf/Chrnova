package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.usecase.BindUnboundDataUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.LoginUseCase
import restarhalf.stellar.schedule.domain.usecase.RunSyncUseCase
import restarhalf.stellar.schedule.domain.usecase.SyncCourseEventsToCalendarUseCase
import restarhalf.stellar.schedule.ui.sync.SyncUiState

/**
 * 应用主ViewModel
 * 
 * 管理应用的核心状态，包括校区、学期开始时间、总周数等。
 * 负责处理同步、登录、登出等全局操作。
 */
class AppViewModel(
    private val auth: AuthPort,
    private val timetable: TimetablePort,
    private val settings: SettingsPort,
    private val fetchExaminations: FetchExaminationsSimpleUseCase,
    private val fetchGrades: FetchGradesSimpleUseCase,
    private val loginUseCase: LoginUseCase,
    private val runSyncUseCase: RunSyncUseCase,
    private val bindUnboundData: BindUnboundDataUseCase,
    private val syncCourseEventsToCalendar: SyncCourseEventsToCalendarUseCase,
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

    /**
     * 应用UI状态数据类
     * 
     * @param campus 当前校区
     * @param termStartMs 学期开始时间戳（毫秒）
     * @param totalWeeks 学期总周数
     */
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
                    onFailure = { e ->
                        if (e is CancellationException) throw e
                        AppLogger.log("Sync", "同步失败", e)
                        SyncUiState.Error(e.toUserFacingMessage(UserFacingErrorKind.Sync))
                    },
                )

        updateState(uiState)
    }

    /** 用户登出，清除认证信息 */
    fun logout() {
        auth.clear()
    }

    /** 校区变更回调 */
    fun onCampusChanged(campus: Campus) {
        timetable.setCampus(campus)
        // 不同校区节次时间表不同，已写入日历的事件时间需要刷新
        resyncCourseCalendar()
    }

    /** 学期开始时间变更回调 */
    fun onTermStartMsChanged(ms: Long) {
        timetable.setTermStartMs(ms)
        // 学期起始日变化会改变所有课程事件的具体日期，需重新写入日历
        resyncCourseCalendar()
    }

    /** 总周数变更回调 */
    fun onTotalWeeksChanged(weeks: Int) {
        timetable.setTotalWeeks(weeks)
        // 总周数变化可能影响事件覆盖范围，重新写入日历
        resyncCourseCalendar()
    }

    /**
     * 重新同步课程事件到日历。
     *
     * 仅在用户已开启"课程日历提醒"时实际写入（由 SyncCourseEventsToCalendarUseCase
     * 内部判断开关），未开启时直接返回 Success(0)，不会误开提醒。
     * 失败仅记录日志，不抛给 UI——日历同步是设置变更的副作用，不应阻断用户操作。
     */
    private fun resyncCourseCalendar() {
        viewModelScope.launch {
            runCatching {
                syncCourseEventsToCalendar(
                    campus = timetable.getCampus(),
                    termStartMs = timetable.getTermStartMs(),
                    totalWeeks = timetable.getTotalWeeks(),
                )
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Calendar", "学期/校区变更后重新同步日历失败", e)
            }
        }
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
