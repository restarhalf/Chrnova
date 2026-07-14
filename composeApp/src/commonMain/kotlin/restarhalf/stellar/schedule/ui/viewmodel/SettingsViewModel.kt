package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.AuthProfile
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.CancelAllCourseRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllExamRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextCourseReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextExamReminderUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 设置页面ViewModel
 * 
 * 管理设置页面的UI状态，包括：
 * - 用户认证状态
 * - 主题模式设置
 * - 提醒设置
 * - 校区和学期选择
 * - 浮动导航栏样式
 */
class SettingsViewModel(
    private val auth: AuthPort,
    private val authWorkflow: AuthWorkflowPort,
    private val settings: SettingsPort,
    private val cancelAllCourseReminders: CancelAllCourseRemindersUseCase,
    private val cancelAllExamReminders: CancelAllExamRemindersUseCase,
    private val fetchSemesterIds: FetchSemesterIdsUseCase,
    private val scheduleNextCourseReminder: ScheduleNextCourseReminderUseCase,
    private val scheduleNextExamReminder: ScheduleNextExamReminderUseCase,
) : ViewModel() {
    /** 设置偏好状态 */
    @Immutable
    private data class SettingsPrefsState(
        val showNonCurrentWeek: Boolean,
        val reminderEnabled: Boolean,
        val examReminderEnabled: Boolean,
        val themeMode: Int,
        val floatingBar: Int,
        val selectedTerm: String,
        val logEnabled: Boolean,
    )

    /** 认证状态 */
    @Immutable
    private data class SettingsAuthState(
        val authToken: String,
        val profile: AuthProfile,
    )

    /**
     * 设置页面UI
     * 
     * @param campusOptions 校区选项列表
     * @param campusSelectedIndex 选中的校区索引
     * @param themeOptions 主题选项列表
     * @param floatingBarOptions 浮动导航栏选项列表
     * @param termStartSummary 学期开始日期摘要
     * @param syncSummary 同步状态摘要
     */
    @Immutable
    data class SettingsScreenUi(
        val campusOptions: List<String>,
        val campusSelectedIndex: Int,
        val themeOptions: List<String>,
        val floatingBarOptions: List<String>,
        val termStartSummary: String,
        val syncSummary: String,
    )

    /**
     * 学期选择UI
     * 
     * @param items 学期选项列表
     * @param selectedIndex 选中的索引
     */
    @Immutable
    data class TermSelectionUi(
        val items: List<String>,
        val selectedIndex: Int,
    )

    /**
     * 账户UI
     * 
     * @param loggedIn 是否已登录
     * @param title 标题（用户名或"已登录"）
     * @param summary 摘要（学号、班级等）
     */
    @Immutable
    data class AccountUi(
        val loggedIn: Boolean,
        val title: String,
        val summary: String,
    )

    /**
     * 设置页面完整UI状态
     * 
     * 包含所有设置项的状态和UI数据。
     */
    @Immutable
    data class SettingsUiState(
        val showNonCurrentWeek: Boolean,
        val reminderEnabled: Boolean,
        val examReminderEnabled: Boolean,
        val themeMode: Int,
        val floatingBar: Int,
        val selectedTerm: String,
        val logEnabled: Boolean,
        val authToken: String,
        val profile: AuthProfile,
        val remoteTermItems: List<String>,
    )

    private companion object {
        val CAMPUS_OPTIONS = listOf("开发区", "金石滩")
        val THEME_OPTIONS = listOf("跟随系统", "浅色", "深色")
        val FLOATING_BAR_OPTIONS = listOf("固定", "悬浮","液态玻璃")
    }

    private val _remoteTermItems = MutableStateFlow<List<String>>(emptyList())

    private val basePrefsFlow = combine(
        settings.observeShowNonCurrentWeek(),
        settings.observeCourseReminderEnabled(),
        settings.observeExamReminderEnabled(),
        settings.observeThemeMode(),
        settings.observeFloatingBar(),
    ) { showNonCurrentWeek, reminderEnabled, examReminderEnabled, themeMode, floatingBar ->
        SettingsPrefsState(
            showNonCurrentWeek = showNonCurrentWeek,
            reminderEnabled = reminderEnabled,
            examReminderEnabled = examReminderEnabled,
            themeMode = themeMode,
            floatingBar = floatingBar,
            selectedTerm = "",
            logEnabled = false,
        )
    }

    private val extraPrefsFlow = combine(
        settings.observeSelectedTerm(),
        settings.observeLogEnabled(),
    ) { selectedTerm, logEnabled ->
        selectedTerm to logEnabled
    }

    private val prefsFlow = combine(
        basePrefsFlow,
        extraPrefsFlow,
    ) { base, (selectedTerm, logEnabled) ->
        base.copy(selectedTerm = selectedTerm, logEnabled = logEnabled)
    }.distinctUntilChanged()

    private val authFlow =
        combine(auth.observeToken(), auth.observeProfile()) { authToken, profile ->
            SettingsAuthState(authToken = authToken, profile = profile)
        }.distinctUntilChanged()

    private val _uiState: StateFlow<SettingsUiState> =
        combine(
            prefsFlow,
            authFlow,
            _remoteTermItems,
        ) { prefs, auth, remoteTermItems ->
            SettingsUiState(
                showNonCurrentWeek = prefs.showNonCurrentWeek,
                reminderEnabled = prefs.reminderEnabled,
                examReminderEnabled = prefs.examReminderEnabled,
                themeMode = prefs.themeMode,
                floatingBar = prefs.floatingBar,
                selectedTerm = prefs.selectedTerm,
                logEnabled = prefs.logEnabled,
                authToken = auth.authToken,
                profile = auth.profile,
                remoteTermItems = remoteTermItems,
            )
        }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    SettingsUiState(
                        showNonCurrentWeek = true,
                        reminderEnabled = false,
                        examReminderEnabled = false,
                        themeMode = 0,
                        floatingBar = 0,
                        selectedTerm = "",
                        logEnabled = false,
                        authToken = "",
                        profile = AuthProfile(),
                        remoteTermItems = emptyList(),
                    ),
            )

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<SettingsUiState> = _uiState

    /** 刷新认证状态 */
    fun refreshAuth() {
        if (uiState.value.authToken.isBlank()) return

        viewModelScope.launch {
            withContext(AppIoDispatcher) {
                runCatching { authWorkflow.ensureLoggedIn() }
                    .onFailure {
                        AppLogger.log("Auth", "刷新认证失败", it)
                    }
            }
        }
    }

    /** 刷新远程学期列表 */
    fun refreshRemoteTerms() {
        if (uiState.value.authToken.isBlank()) {
            _remoteTermItems.value = emptyList()
            return
        }

        viewModelScope.launch {
            runCatching {
                val terms =
                    withContext(AppIoDispatcher) {
                        fetchSemesterIds()
                    }
                _remoteTermItems.value = terms
            }.onFailure {
                AppLogger.log("Settings", "刷新远程学期列表失败", it)
            }
        }
    }

    /**
     * 构建学期选择UI
     * 
     * @param authToken 认证令牌
     * @param remoteTermItems 远程学期列表
     * @param selectedTerm 当前选中的学期
     * @return 学期选择UI
     */
    fun buildTermSelectionUi(
        authToken: String,
        remoteTermItems: List<String>,
        selectedTerm: String,
    ): TermSelectionUi {
        val items =
            if (authToken.isBlank()) {
                listOf("当前学期")
            } else {
                listOf("当前学期") + remoteTermItems
            }
        val index = items.indexOf(selectedTerm).takeIf { it >= 0 } ?: 0
        return TermSelectionUi(items = items, selectedIndex = index)
    }

    /**
     * 构建账户UI
     * 
     * @param authToken 认证令牌
     * @param profile 用户档案
     * @return 账户UI
     */
    fun buildAccountUi(authToken: String, profile: AuthProfile): AccountUi {
        val loggedIn =
            authToken.isNotBlank() && (profile.name.isNotBlank() || profile.userNo.isNotBlank())
        val summary =
            buildString {
                append(profile.userNo)
                val extra = listOf(profile.clsName, profile.academyName).filter { it.isNotBlank() }
                if (extra.isNotEmpty()) {
                    if (profile.userNo.isNotBlank()) append("\n")
                    append(extra.joinToString(" · "))
                }
            }
                .ifBlank { "用于获取课表" }
        return AccountUi(
            loggedIn = loggedIn,
            title = profile.name.ifBlank { "已登录" },
            summary = summary
        )
    }

    /**
     * 构建设置页面UI
     * 
     * @param syncUiState 同步状态
     * @param campus 当前校区
     * @param termStartMs 学期开始时间戳
     * @return 设置页面UI
     */
    fun buildScreenUi(
        syncUiState: SyncUiState,
        campus: Campus,
        termStartMs: Long,
    ): SettingsScreenUi {
        val campusSelectedIndex =
            when (campus) {
                Campus.Development -> 0
                Campus.Jinshitan -> 1
            }
        val termStartSummary = formatDateYmd(termStartMs)
        val syncSummary =
            when (syncUiState) {
                is SyncUiState.Idle -> "立即从教务拉取最新课表"
                is SyncUiState.Loading -> "同步中..."
                is SyncUiState.Success -> "同步成功：${syncUiState.inserted} 门（${syncUiState.campusName}）"
                is SyncUiState.Error -> syncUiState.message
            }
        return SettingsScreenUi(
            campusOptions = CAMPUS_OPTIONS,
            campusSelectedIndex = campusSelectedIndex,
            themeOptions = THEME_OPTIONS,
            floatingBarOptions = FLOATING_BAR_OPTIONS,
            termStartSummary = termStartSummary,
            syncSummary = syncSummary
        )
    }

    /**
     * 根据索引获取学期值
     * 
     * @param items 学期选项列表
     * @param index 索引
     * @return 学期值，"当前学期"返回空字符串
     */
    fun selectedTermValueFromIndex(items: List<String>, index: Int): String {
        val value = items.getOrNull(index).orEmpty()
        return if (value == "当前学期") "" else value
    }

    /**
     * 根据索引获取校区
     * 
     * @param index 索引
     * @return 校区枚举
     */
    fun campusFromIndex(index: Int): Campus {
        return if (index == 0) Campus.Development else Campus.Jinshitan
    }

    /**
     * 选中的学期变更回调
     * 
     * @param value 学期值
     */
    fun onSelectedTermChanged(value: String) {
        viewModelScope.launch {
            settings.setSelectedTerm(value)
        }
    }

    /**
     * 主题模式变更回调
     * 
     * @param mode 主题模式（0=跟随系统，1=浅色，2=深色）
     */
    fun onThemeModeChanged(mode: Int) {
        settings.setThemeMode(mode)
    }

    /**
     * 浮动导航栏模式变更回调
     * 
     * @param mode 模式（0=固定，1=悬浮，2=液态玻璃）
     */
    fun onFloatingBarChanged(mode: Int) {
        settings.setFloatingBar(mode)
    }

    /**
     * 是否显示非本周课程变更回调
     * 
     * @param show 是否显示
     */
    fun onShowNonCurrentWeekChanged(show: Boolean) {
        settings.setShowNonCurrentWeek(show)
    }

    /**
     * 课程提醒开关变更回调
     * 
     * @param enabled 是否启用
     */
    fun onReminderEnabledChanged(enabled: Boolean) {
        settings.setCourseReminderEnabled(enabled)
        if (!enabled) {
            cancelAllCourseReminders()
        }
    }

    /**
     * 考试提醒开关变更回调
     * 
     * @param enabled 是否启用
     */
    fun onExamReminderEnabledChanged(enabled: Boolean) {
        settings.setExamReminderEnabled(enabled)
        if (!enabled) {
            cancelAllExamReminders()
        }
    }

    fun onLogEnabledChanged(enabled: Boolean) {
        settings.setLogEnabled(enabled)
    }

    /**
     * 调度课程提醒
     * 
     * @param campus 当前校区
     * @param termStartMs 学期开始时间戳
     * @param totalWeeks 学期总周数
     */
    fun scheduleCourseReminder(campus: Campus, termStartMs: Long, totalWeeks: Int) {
        viewModelScope.launch {
            val result =
                withContext(AppIoDispatcher) {
                    runCatching {
                        scheduleNextCourseReminder(
                            campus = campus,
                            termStartMs = termStartMs,
                            totalWeeks = totalWeeks
                        )
                    }
                }
            if (result.isFailure) {
                AppLogger.log("Reminder", "课程提醒调度失败", result.exceptionOrNull()!!)
                onReminderEnabledChanged(false)
            }
        }
    }

    /**
     * 调度考试提醒
     * 
     * @param selectedTerm 选中的学期
     */
    fun scheduleExamReminder(selectedTerm: String) {
        viewModelScope.launch {
            val result =
                withContext(AppIoDispatcher) {
                    runCatching {
                        scheduleNextExamReminder(selectedTerm = selectedTerm)
                    }
                }
            if (result.isFailure) {
                AppLogger.log("Reminder", "考试提醒调度失败", result.exceptionOrNull()!!)

            }
        }
    }

    /**
     * 格式化日期为YYYY/MM/DD
     * 
     * @param ms 时间戳（毫秒）
     * @return 格式化的日期字符串
     */
    @OptIn(ExperimentalTime::class)
    private fun formatDateYmd(ms: Long): String {
        val date =
            Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).date
        val month = (date.month.ordinal + 1).toString().padStart(2, '0')
        val day = date.day.toString().padStart(2, '0')
        return "${date.year}/$month/$day"
    }
}
