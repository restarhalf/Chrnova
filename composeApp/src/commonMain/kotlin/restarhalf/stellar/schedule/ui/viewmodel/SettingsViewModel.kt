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
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.ExactAlarmPermissionState
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.port.ExactAlarmPermissionPort
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.port.ReminderPermissionPort
import restarhalf.stellar.schedule.domain.port.ReminderPermissionState
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.port.UserProfilePort
import restarhalf.stellar.schedule.domain.usecase.CancelAllCourseRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllExamRemindersUseCase
import restarhalf.stellar.schedule.domain.reminder.ClassroomLabel
import restarhalf.stellar.schedule.domain.usecase.ClassroomLabelPreview
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.domain.usecase.LoadClassroomLabelPreviewUseCase
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
    private val exactAlarmPermission: ExactAlarmPermissionPort,
    private val reminderPermission: ReminderPermissionPort,
    private val termSettings: TermSettingsPort,
    private val userProfile: UserProfilePort,
    private val deviceSettings: DeviceSettingsPort,
    private val cancelAllCourseReminders: CancelAllCourseRemindersUseCase,
    private val cancelAllExamReminders: CancelAllExamRemindersUseCase,
    private val fetchSemesterIds: FetchSemesterIdsUseCase,
    private val scheduleNextCourseReminder: ScheduleNextCourseReminderUseCase,
    private val scheduleNextExamReminder: ScheduleNextExamReminderUseCase,
    private val courseReminderPort: CourseReminderPort,
    private val loadLabelPreview: LoadClassroomLabelPreviewUseCase,
    verifyGitHubStar: VerifyGitHubStarUseCase,
) : ViewModel() {

    private val _exactAlarmState = MutableStateFlow(ExactAlarmPermissionState.GRANTED)
    val exactAlarmState: StateFlow<ExactAlarmPermissionState> = _exactAlarmState

    fun refreshExactAlarmState() {
        _exactAlarmState.value =
            when {
                exactAlarmPermission.isExactAlarmAllowed() -> ExactAlarmPermissionState.GRANTED
                exactAlarmPermission.hasAsked() -> ExactAlarmPermissionState.DENIED
                else -> ExactAlarmPermissionState.NOT_ASKED
            }
    }

    private val _reminderPermissionState = MutableStateFlow(ReminderPermissionState())
    val reminderPermissionState: StateFlow<ReminderPermissionState> = _reminderPermissionState

    fun refreshReminderPermissionState() {
        _reminderPermissionState.value = reminderPermission.snapshot()
    }

    fun openBatteryOptimizationSettings() {
        reminderPermission.openBatteryOptimizationSettings()
    }

    fun openAppDetailsSettings() {
        reminderPermission.openAppDetailsSettings()
    }

    fun openDndPolicySettings() {
        reminderPermission.openDndPolicySettings()
    }

    fun requestShizukuPermission() {
        reminderPermission.requestShizukuPermission { granted ->
            _reminderPermissionState.value = reminderPermission.snapshot()
            if (!granted) _userMessages.tryEmit("Shizuku 授权失败")
        }
    }

    /** 测试按钮：岛可用且开启时发岛，否则发实况；两者都会带上测试课的自动勿扰。 */
    fun sendTestReminderNotification(island: Boolean) {
        reminderPermission.sendTestNotification(island)
        _userMessages.tryEmit(
            if (island) {
                "超级岛测试已发送，70 秒后上课"
            } else {
                "实时活动测试已发送，70 秒后上课"
            },
        )
    }

    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val userMessages: SharedFlow<String> = _userMessages

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
        val selectedTerm: String,
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
        val selectedTerm: String,
        val authToken: String,
        val profile: JwxtAuthProfile,
        val remoteTermItems: ImmutableList<String>,
    )

    private companion object {
        val CAMPUS_OPTIONS = persistentListOf("开发区", "金石滩")
    }

    private val _remoteTermItems = MutableStateFlow<ImmutableList<String>>(persistentListOf())

    private val basePrefsFlow = combine(
        displaySettings.observeShowNonCurrentWeek(),
        reminderSettings.observeCourseReminderEnabled(),
    ) { showNonCurrentWeek, reminderEnabled ->
        SettingsPrefsState(
            showNonCurrentWeek = showNonCurrentWeek,
            scheduleRowHeight = DisplaySettingsPort.DEFAULT_ROW_HEIGHT_DP,
            reminderEnabled = reminderEnabled,
            selectedTerm = "",
        )
    }

    private val extraPrefsFlow = combine(
        termSettings.observeSelectedTerm(),
        displaySettings.observeScheduleRowHeight(),
    ) { selectedTerm, scheduleRowHeight ->
        selectedTerm to scheduleRowHeight
    }

    private val prefsFlow = combine(
        basePrefsFlow,
        extraPrefsFlow,
    ) { base, (selectedTerm, scheduleRowHeight) ->
        base.copy(
            selectedTerm = selectedTerm,
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
                selectedTerm = prefs.selectedTerm,
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
                        selectedTerm = "",
                        authToken = "",
                        profile = JwxtAuthProfile(),
                        remoteTermItems = persistentListOf(),
                    ),
            )

    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            authFlow.collect { auth ->
                if (auth.authToken.isBlank()) resetTermIfMissing(emptyList())
            }
        }
        refreshExtraReminders()
    }

    fun refreshAuth() {
        viewModelScope.launch {
            withContext(AppIoDispatcher) {
                runCatching { authWorkflow.ensureLoggedIn() }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                    }
            }
        }
    }

    fun refreshRemoteTerms() {
        viewModelScope.launch {
            val authToken = auth.observeToken().first()
            if (authToken.isBlank()) {
                _remoteTermItems.value = persistentListOf()
                return@launch
            }

            runCatching {
                val terms =
                    withContext(AppIoDispatcher) {
                        fetchSemesterIds()
                    }
                _remoteTermItems.value = terms.toPersistentList()
                resetTermIfMissing(terms)
            }.onFailure { e ->
                if (e is CancellationException) throw e
            }
        }
    }

    private suspend fun resetTermIfMissing(terms: List<String>) {
        val current = termSettings.observeSelectedTerm().first()
        if (current.isBlank() || current in terms) return
        onSelectedTermChanged("")
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
                }
        }
    }

    fun onThemeModeChanged(mode: Int) {
        displaySettings.setThemeMode(mode)
    }

    fun onShowNonCurrentWeekChanged(show: Boolean) {
        displaySettings.setShowNonCurrentWeek(show)
    }

    fun onScheduleRowHeightChanged(heightDp: Int) {
        displaySettings.setScheduleRowHeight(heightDp)
    }

    val reminderLeadMinutes: StateFlow<Int> =
        reminderSettings.observeReminderLeadMinutes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = restarhalf.stellar.schedule.domain.reminder.ReminderTiming.LEAD_MINUTES,
            )

    fun onReminderLeadMinutesChanged(minutes: Int) {
        reminderSettings.setReminderLeadMinutes(minutes)
    }

    val nextDayReminderEnabled: StateFlow<Boolean> =
        reminderSettings.observeNextDayReminderEnabled()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val nextDayReminderMinuteOfDay: StateFlow<Int> =
        reminderSettings.observeNextDayReminderMinuteOfDay()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                restarhalf.stellar.schedule.domain.reminder.ReminderTiming.NEXT_DAY_MINUTE_OF_DAY,
            )

    val inClassReminderEnabled: StateFlow<Boolean> =
        reminderSettings.observeInClassReminderEnabled()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val inClassTimingMode: StateFlow<Int> =
        reminderSettings.observeInClassTimingMode()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val inClassLeadMinutes: StateFlow<Int> =
        reminderSettings.observeInClassLeadMinutes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 10)

    val classDndEnabled: StateFlow<Boolean> =
        reminderSettings.observeClassDndEnabled()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val classDndMode: StateFlow<Int> =
        reminderSettings.observeClassDndMode()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val inExamReminderEnabled: StateFlow<Boolean> =
        reminderSettings.observeInExamReminderEnabled()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val inExamTimingMode: StateFlow<Int> =
        reminderSettings.observeInExamTimingMode()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val inExamLeadMinutes: StateFlow<Int> =
        reminderSettings.observeInExamLeadMinutes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 10)

    val examDndEnabled: StateFlow<Boolean> =
        reminderSettings.observeExamDndEnabled()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val examDndMode: StateFlow<Int> =
        reminderSettings.observeExamDndMode()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val islandEnabled: StateFlow<Boolean> =
        reminderSettings.observeIslandEnabled()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val islandLeftMode: StateFlow<Int> =
        reminderSettings.observeIslandLeftMode()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val islandRightMode: StateFlow<Int> =
        reminderSettings.observeIslandRightMode()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val islandAodMode: StateFlow<Int> =
        reminderSettings.observeIslandAodMode()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onIslandEnabledChanged(enabled: Boolean) {
        reminderSettings.setIslandEnabled(enabled)
    }

    fun onIslandLeftModeChanged(mode: Int) {
        reminderSettings.setIslandLeftMode(mode)
    }

    fun onIslandRightModeChanged(mode: Int) {
        reminderSettings.setIslandRightMode(mode)
    }

    fun onIslandAodModeChanged(mode: Int) {
        reminderSettings.setIslandAodMode(mode)
    }

    val classroomLabelScope: StateFlow<Int> =
        reminderSettings.observeClassroomLabelScope()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClassroomLabel.SCOPE_OFF)

    val classroomLabelMethod: StateFlow<Int> =
        reminderSettings.observeClassroomLabelMethod()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClassroomLabel.METHOD_REGEX)

    val classroomLabelAbbreviations: StateFlow<String> =
        reminderSettings.observeClassroomLabelAbbreviations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val classroomLabelOverrides: StateFlow<String> =
        reminderSettings.observeClassroomLabelOverrides()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _classroomLabelPreview = MutableStateFlow(ClassroomLabelPreview())
    val classroomLabelPreview: StateFlow<ClassroomLabelPreview> = _classroomLabelPreview

    fun loadClassroomLabelPreview() {
        viewModelScope.launch {
            _classroomLabelPreview.value =
                runCatching { loadLabelPreview() }.getOrDefault(ClassroomLabelPreview())
        }
    }

    fun onClassroomLabelScopeChanged(scope: Int) {
        reminderSettings.setClassroomLabelScope(scope)
    }

    fun onClassroomLabelMethodChanged(method: Int) {
        reminderSettings.setClassroomLabelMethod(method)
    }

    fun onClassroomLabelAbbreviationsChanged(text: String) {
        reminderSettings.setClassroomLabelAbbreviations(text)
    }

    fun onClassroomLabelOverridesChanged(text: String) {
        reminderSettings.setClassroomLabelOverrides(text)
    }

    fun onNextDayReminderEnabledChanged(enabled: Boolean) {
        reminderSettings.setNextDayReminderEnabled(enabled)
        refreshExtraReminders()
    }

    fun onNextDayReminderMinuteChanged(minuteOfDay: Int) {
        reminderSettings.setNextDayReminderMinuteOfDay(minuteOfDay)
        refreshExtraReminders()
    }

    fun onInClassReminderEnabledChanged(enabled: Boolean) {
        reminderSettings.setInClassReminderEnabled(enabled)
    }

    fun onInClassTimingModeChanged(mode: Int) {
        reminderSettings.setInClassTimingMode(mode)
    }

    fun onInClassLeadMinutesChanged(minutes: Int) {
        reminderSettings.setInClassLeadMinutes(minutes)
    }

    fun onClassDndEnabledChanged(enabled: Boolean) {
        reminderSettings.setClassDndEnabled(enabled)
    }

    fun onClassDndModeChanged(mode: Int) {
        reminderSettings.setClassDndMode(mode)
    }

    fun onInExamReminderEnabledChanged(enabled: Boolean) {
        reminderSettings.setInExamReminderEnabled(enabled)
    }

    fun onInExamTimingModeChanged(mode: Int) {
        reminderSettings.setInExamTimingMode(mode)
    }

    fun onInExamLeadMinutesChanged(minutes: Int) {
        reminderSettings.setInExamLeadMinutes(minutes)
    }

    fun onExamDndEnabledChanged(enabled: Boolean) {
        reminderSettings.setExamDndEnabled(enabled)
    }

    fun onExamDndModeChanged(mode: Int) {
        reminderSettings.setExamDndMode(mode)
    }

    private fun refreshExtraReminders() {
        viewModelScope.launch {
            withContext(AppIoDispatcher) {
                runCatching { courseReminderPort.refreshNextDayReminder() }
                    .onFailure { if (it is CancellationException) throw it }
            }
        }
    }

    fun onReminderEnabledChanged(enabled: Boolean) {
        reminderSettings.setCourseReminderEnabled(enabled)
        if (!enabled) {
            viewModelScope.launch {
                withContext(AppIoDispatcher) {
                    // 考试提醒沿用课程提醒：关总开关时一并取消
                    runCatching { cancelAllCourseReminders() }
                    runCatching { cancelAllExamReminders() }
                }
            }
        }
    }

    fun triggerSync(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { e ->
                    if (e is CancellationException) throw e
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
            val e = result.exceptionOrNull()
            when {
                e is CancellationException -> throw e
                e != null -> {
                    _userMessages.emit("课程提醒没排上，稍后会自动重试")
                }
                result.getOrNull() == ReminderScheduleResult.Failed -> {
                    _userMessages.emit("课程提醒没排上，稍后会自动重试")
                }
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
            val e = result.exceptionOrNull()
            when {
                e is CancellationException -> throw e
                e != null -> {
                    _userMessages.emit("考试提醒没排上，稍后会自动重试")
                }
                result.getOrNull() == ReminderScheduleResult.Failed -> {
                    _userMessages.emit("考试提醒没排上，稍后会自动重试")
                }
            }
        }
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
