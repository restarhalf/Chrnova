package restarhalf.stellar.schedule.ui.viewmodel

import restarhalf.stellar.schedule.ui.model.TermSelectionUi
import restarhalf.stellar.schedule.ui.model.SettingsScreenUi
import restarhalf.stellar.schedule.ui.model.AccountUi
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.port.LogSettingsPort
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.port.UserProfilePort
import restarhalf.stellar.schedule.domain.usecase.CancelAllCourseRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllExamRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleAllRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextCourseReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextExamReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class SettingsViewModel(
    private val auth: JwxtAuthPort,
    private val authWorkflow: JwxtAuthWorkflowPort,
    private val displaySettings: DisplaySettingsPort,
    private val reminderSettings: ReminderSettingsPort,
    private val termSettings: TermSettingsPort,
    private val logSettings: LogSettingsPort,
    private val userProfile: UserProfilePort,
    private val deviceSettings: DeviceSettingsPort,
    private val cancelAllCourseReminders: CancelAllCourseRemindersUseCase,
    private val cancelAllExamReminders: CancelAllExamRemindersUseCase,
    private val fetchSemesterIds: FetchSemesterIdsUseCase,
    private val scheduleNextCourseReminder: ScheduleNextCourseReminderUseCase,
    private val scheduleNextExamReminder: ScheduleNextExamReminderUseCase,
    private val rescheduleAllReminders: RescheduleAllRemindersUseCase,
    private val courseReminderPort: CourseReminderPort,
    private val examReminderPort: ExamReminderPort,
    verifyGitHubStar: VerifyGitHubStarUseCase,
) : ViewModel() {

    private val _rescheduling = MutableStateFlow(false)
    val rescheduling: StateFlow<Boolean> = _rescheduling

    /** 「重新排提醒」下面的那行小字：当前实际挂着的下一条提醒 */
    private val _nextReminderSummary = MutableStateFlow("")
    val nextReminderSummary: StateFlow<String> = _nextReminderSummary

    private val _rescheduleMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val rescheduleMessages: SharedFlow<String> = _rescheduleMessages

    val starVerification = StarVerificationHolder(
        verifyGitHubStar = verifyGitHubStar,
        settings = deviceSettings,
        scope = viewModelScope,
    )

    @Immutable
    private data class SettingsPrefsState(
        val showNonCurrentWeek: Boolean,
        val scheduleRowHeight: Int,
        val reminderEnabled: Boolean,
        val examReminderEnabled: Boolean,
        val themeMode: Int,
        val floatingBar: Int,
        val selectedTerm: String,
        val logEnabled: Boolean,
    )

    @Immutable
    private data class SettingsAuthState(
        val authToken: String,
        val profile: JwxtAuthProfile,
    )

    @Stable
    data class SettingsUiState(
        val showNonCurrentWeek: Boolean,
        val scheduleRowHeight: Int,
        val reminderEnabled: Boolean,
        val examReminderEnabled: Boolean,
        val themeMode: Int,
        val floatingBar: Int,
        val selectedTerm: String,
        val logEnabled: Boolean,
        val authToken: String,
        val profile: JwxtAuthProfile,
        val remoteTermItems: ImmutableList<String>,
    )

    private companion object {
        val CAMPUS_OPTIONS = persistentListOf("开发区", "金石滩")
        val THEME_OPTIONS = persistentListOf("跟随系统", "浅色", "深色")
        val FLOATING_BAR_OPTIONS = persistentListOf("固定", "悬浮","液态玻璃")
    }

    private val _remoteTermItems = MutableStateFlow<ImmutableList<String>>(persistentListOf())

    private val basePrefsFlow = combine(
        displaySettings.observeShowNonCurrentWeek(),
        reminderSettings.observeCourseReminderEnabled(),
        reminderSettings.observeExamReminderEnabled(),
        displaySettings.observeThemeMode(),
        displaySettings.observeFloatingBar(),
    ) { showNonCurrentWeek, reminderEnabled, examReminderEnabled, themeMode, floatingBar ->
        SettingsPrefsState(
            showNonCurrentWeek = showNonCurrentWeek,
            scheduleRowHeight = DisplaySettingsPort.DEFAULT_ROW_HEIGHT_DP,
            reminderEnabled = reminderEnabled,
            examReminderEnabled = examReminderEnabled,
            themeMode = themeMode,
            floatingBar = floatingBar,
            selectedTerm = "",
            logEnabled = false,
        )
    }

    private val extraPrefsFlow = combine(
        termSettings.observeSelectedTerm(),
        logSettings.observeLogEnabled(),
        displaySettings.observeScheduleRowHeight(),
    ) { selectedTerm, logEnabled, scheduleRowHeight ->
        Triple(selectedTerm, logEnabled, scheduleRowHeight)
    }

    private val prefsFlow = combine(
        basePrefsFlow,
        extraPrefsFlow,
    ) { base, (selectedTerm, logEnabled, scheduleRowHeight) ->
        base.copy(
            selectedTerm = selectedTerm,
            logEnabled = logEnabled,
            scheduleRowHeight = scheduleRowHeight,
        )
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
                scheduleRowHeight = prefs.scheduleRowHeight,
                reminderEnabled = prefs.reminderEnabled,
                examReminderEnabled = prefs.examReminderEnabled,
                themeMode = prefs.themeMode,
                floatingBar = prefs.floatingBar,
                selectedTerm = prefs.selectedTerm,
                logEnabled = prefs.logEnabled,
                authToken = auth.authToken,
                profile = auth.profile,
                remoteTermItems = remoteTermItems.toPersistentList(),
            )
        }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    SettingsUiState(
                        showNonCurrentWeek = true,
                        scheduleRowHeight = DisplaySettingsPort.DEFAULT_ROW_HEIGHT_DP,
                        reminderEnabled = false,
                        examReminderEnabled = false,
                        themeMode = 0,
                        floatingBar = 0,
                        selectedTerm = "",
                        logEnabled = false,
                        authToken = "",
                        profile = JwxtAuthProfile(),
                        remoteTermItems = persistentListOf(),
                    ),
            )

    val uiState: StateFlow<SettingsUiState> = _uiState

    fun refreshAuth() {
        if (uiState.value.authToken.isBlank()) return

        viewModelScope.launch {
            withContext(AppIoDispatcher) {
                runCatching { authWorkflow.ensureLoggedIn() }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        AppLogger.log("Auth", "刷新认证失败", e)
                    }
            }
        }
    }

    fun refreshRemoteTerms() {
        if (uiState.value.authToken.isBlank()) {
            _remoteTermItems.value = persistentListOf()
            return
        }

        viewModelScope.launch {
            runCatching {
                val terms =
                    withContext(AppIoDispatcher) {
                        fetchSemesterIds()
                    }
                _remoteTermItems.value = terms.toPersistentList()
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Settings", "刷新远程学期列表失败", e)
            }
        }
    }

    fun buildTermSelectionUi(
        authToken: String,
        remoteTermItems: List<String>,
        selectedTerm: String,
    ): TermSelectionUi {
        val items =
            if (authToken.isBlank()) {
                persistentListOf("当前学期")
            } else {
                (listOf("当前学期") + remoteTermItems).toPersistentList()
            }
        val index = items.indexOf(selectedTerm).takeIf { it >= 0 } ?: 0
        return TermSelectionUi(items = items, selectedIndex = index)
    }

    fun buildAccountUi(authToken: String, profile: JwxtAuthProfile): AccountUi {
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
            campusOptions = CAMPUS_OPTIONS.toPersistentList(),
            campusSelectedIndex = campusSelectedIndex,
            themeOptions = THEME_OPTIONS.toPersistentList(),
            floatingBarOptions = FLOATING_BAR_OPTIONS.toPersistentList(),
            termStartSummary = termStartSummary,
            syncSummary = syncSummary
        )
    }

    fun selectedTermValueFromIndex(items: List<String>, index: Int): String {
        val value = items.getOrNull(index).orEmpty()
        return if (value == "当前学期") "" else value
    }

    fun campusFromIndex(index: Int): Campus {
        return if (index == 0) Campus.Development else Campus.Jinshitan
    }

    fun onSelectedTermChanged(value: String) {
        viewModelScope.launch {
            termSettings.setSelectedTerm(value)
            termSettings.setActiveScheduleTerm(value)

            runCatching { scheduleNextExamReminder(selectedTerm = value) }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Reminder", "切换学期后重新调度考试提醒失败", e)
                }
        }
    }

    fun onThemeModeChanged(mode: Int) {
        displaySettings.setThemeMode(mode)
    }

    fun onFloatingBarChanged(mode: Int) {
        displaySettings.setFloatingBar(mode)
    }

    fun onShowNonCurrentWeekChanged(show: Boolean) {
        displaySettings.setShowNonCurrentWeek(show)
    }

    fun onScheduleRowHeightChanged(heightDp: Int) {
        displaySettings.setScheduleRowHeight(heightDp)
    }

    fun onReminderEnabledChanged(enabled: Boolean) {
        reminderSettings.setCourseReminderEnabled(enabled)
        if (!enabled) {
            viewModelScope.launch {
                withContext(AppIoDispatcher) {
                    runCatching { cancelAllCourseReminders() }
                }
                refreshNextReminderSummary()
            }
        } else {
            refreshNextReminderSummary()
        }
    }

    fun onExamReminderEnabledChanged(enabled: Boolean) {
        reminderSettings.setExamReminderEnabled(enabled)
        if (!enabled) {
            viewModelScope.launch {
                withContext(AppIoDispatcher) {
                    runCatching { cancelAllExamReminders() }
                }
                refreshNextReminderSummary()
            }
        } else {
            refreshNextReminderSummary()
        }
    }

    fun onLogEnabledChanged(enabled: Boolean) {
        logSettings.setLogEnabled(enabled)
    }

    fun triggerSync(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Sync", "同步失败", e)
                }
        }
    }

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
                val e = result.exceptionOrNull()!!
                if (e is CancellationException) throw e
                AppLogger.log("Reminder", "课程提醒调度失败", e)
                onReminderEnabledChanged(false)
            }
        }
    }

    fun scheduleExamReminder(selectedTerm: String) {
        viewModelScope.launch {
            val result =
                withContext(AppIoDispatcher) {
                    runCatching {
                        scheduleNextExamReminder(selectedTerm = selectedTerm)
                    }
                }
            if (result.isFailure) {
                val e = result.exceptionOrNull()!!
                if (e is CancellationException) throw e
                AppLogger.log("Reminder", "考试提醒调度失败", e)
            }
        }
    }

    /** 一键重排：清掉现有闹钟，按当前课表与考试重新算一遍 */
    fun onRescheduleAllRemindersClicked() {
        if (_rescheduling.value) return
        _rescheduling.value = true
        viewModelScope.launch {
            val report =
                withContext(AppIoDispatcher) {
                    runCatching { rescheduleAllReminders() }
                        .onFailure {
                            if (it is CancellationException) throw it
                            AppLogger.log("Reminder", "一键重排失败", it)
                        }
                        .getOrNull()
                }
            _rescheduling.value = false
            refreshNextReminderSummary()
            _rescheduleMessages.emit(describeReschedule(report))
        }
    }

    /** 重新读一次两条链上实际挂着的闹钟，更新「重新排提醒」下面的小字 */
    fun refreshNextReminderSummary() {
        viewModelScope.launch {
            val summary =
                withContext(AppIoDispatcher) {
                    val courseEnabled = reminderSettings.observeCourseReminderEnabled().first()
                    val examEnabled = reminderSettings.observeExamReminderEnabled().first()
                    buildNextReminderSummary(
                        courseEnabled = courseEnabled,
                        courseNextAtMs =
                            if (courseEnabled) {
                                runCatching { courseReminderPort.nextScheduledAtMs() }.getOrNull()
                            } else {
                                null
                            },
                        examEnabled = examEnabled,
                        examNextAtMs =
                            if (examEnabled) {
                                runCatching { examReminderPort.nextScheduledAtMs() }.getOrNull()
                            } else {
                                null
                            },
                    )
                }
            _nextReminderSummary.value = summary
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun buildNextReminderSummary(
        courseEnabled: Boolean,
        courseNextAtMs: Long?,
        examEnabled: Boolean,
        examNextAtMs: Long?,
    ): String {
        if (!courseEnabled && !examEnabled) return "提醒都关着"

        val lines = mutableListOf<String>()
        if (courseEnabled) {
            lines += "课程 " + describeNext(courseNextAtMs)
        }
        if (examEnabled) {
            lines += "考试 " + describeNext(examNextAtMs)
        }
        return lines.joinToString(" · ")
    }

    private fun describeNext(triggerAtMs: Long?): String =
        if (triggerAtMs == null) "未排上" else describeResult(triggerAtMs).removePrefix("下次 ")

    @OptIn(ExperimentalTime::class)
    private fun describeReschedule(report: RescheduleAllRemindersUseCase.Report?): String {
        if (report == null) return "重排失败，请稍后重试"
        if (!report.courseEnabled && !report.examEnabled) return "提醒都关着，没有需要重排的项目"

        val lines = mutableListOf<String>()
        if (report.courseEnabled) {
            lines += "课程提醒：" + describeResult(report.courseResult)
        }
        if (report.examEnabled) {
            lines +=
                "考试提醒：" +
                    if (report.examFetchFailed) "拉取考试安排失败" else describeResult(report.examResult)
        }
        return lines.joinToString("\n")
    }

    @OptIn(ExperimentalTime::class)
    private fun describeResult(triggerAtMs: Long?): String {
        if (triggerAtMs == null) return "调度失败"
        val dateTime =
            Instant.fromEpochMilliseconds(triggerAtMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        val month = dateTime.month.ordinal + 1
        val minute = dateTime.minute.toString().padStart(2, '0')
        val hour = dateTime.hour.toString().padStart(2, '0')
        return "下次 $month/${dateTime.day} $hour:$minute"
    }

    private fun describeResult(result: CourseReminderPort.ScheduleResult?): String =
        when (result) {
            is CourseReminderPort.ScheduleResult.Scheduled -> describeResult(result.triggerAtMs)
            CourseReminderPort.ScheduleResult.NoUpcoming -> "近期没有课"
            CourseReminderPort.ScheduleResult.Failed, null -> "调度失败"
        }

    private fun describeResult(result: ExamReminderPort.ScheduleResult?): String =
        when (result) {
            is ExamReminderPort.ScheduleResult.Scheduled -> describeResult(result.triggerAtMs)
            ExamReminderPort.ScheduleResult.NoUpcoming -> "近期没有考试"
            ExamReminderPort.ScheduleResult.Failed, null -> "调度失败"
        }

    @OptIn(ExperimentalTime::class)
    private fun formatDateYmd(ms: Long): String {
        val date =
            Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).date
        val month = (date.month.ordinal + 1).toString().padStart(2, '0')
        val day = date.day.toString().padStart(2, '0')
        return "${date.year}/$month/$day"
    }

    @Immutable
    data class PersonalInfoUiState(
        val avatarUri: String? = null,
        val nickname: String? = null,
        val hasPersonalInfo: Boolean = false,
    )

    private val _personalInfoUiState = MutableStateFlow(PersonalInfoUiState())
    val personalInfoUiState: StateFlow<PersonalInfoUiState> = _personalInfoUiState

    fun loadPersonalInfo() {
        viewModelScope.launch {
            val avatarUri = userProfile.getUserAvatarUri()
            val nickname = userProfile.getUserNickname()
            _personalInfoUiState.value = PersonalInfoUiState(
                avatarUri = avatarUri,
                nickname = nickname,
                hasPersonalInfo = avatarUri != null || nickname != null,
            )
        }
    }
}
