package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
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
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.CancelAllCourseRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllExamRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
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
    private val settings: SettingsPort,
    private val cancelAllCourseReminders: CancelAllCourseRemindersUseCase,
    private val cancelAllExamReminders: CancelAllExamRemindersUseCase,
    private val fetchSemesterIds: FetchSemesterIdsUseCase,
    private val scheduleNextCourseReminder: ScheduleNextCourseReminderUseCase,
    private val scheduleNextExamReminder: ScheduleNextExamReminderUseCase,
    verifyGitHubStar: VerifyGitHubStarUseCase,
) : ViewModel() {

    val starVerification = StarVerificationHolder(
        verifyGitHubStar = verifyGitHubStar,
        settings = settings,
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
    data class SettingsScreenUi(
        val campusOptions: ImmutableList<String>,
        val campusSelectedIndex: Int,
        val themeOptions: ImmutableList<String>,
        val floatingBarOptions: ImmutableList<String>,
        val termStartSummary: String,
        val syncSummary: String,
    )

    
    @Stable
    data class TermSelectionUi(
        val items: ImmutableList<String>,
        val selectedIndex: Int,
    )

    
    @Immutable
    data class AccountUi(
        val loggedIn: Boolean,
        val title: String,
        val summary: String,
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
        settings.observeShowNonCurrentWeek(),
        settings.observeCourseReminderEnabled(),
        settings.observeExamReminderEnabled(),
        settings.observeThemeMode(),
        settings.observeFloatingBar(),
    ) { showNonCurrentWeek, reminderEnabled, examReminderEnabled, themeMode, floatingBar ->
        SettingsPrefsState(
            showNonCurrentWeek = showNonCurrentWeek,
            scheduleRowHeight = SettingsPort.DEFAULT_ROW_HEIGHT_DP,
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
        settings.observeScheduleRowHeight(),
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
                        scheduleRowHeight = SettingsPort.DEFAULT_ROW_HEIGHT_DP,
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
            settings.setSelectedTerm(value)
            settings.setActiveScheduleTerm(value)

            runCatching { scheduleNextExamReminder(selectedTerm = value) }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Reminder", "切换学期后重新调度考试提醒失败", e)
                }
        }
    }

    
    fun onThemeModeChanged(mode: Int) {
        settings.setThemeMode(mode)
    }

    
    fun onFloatingBarChanged(mode: Int) {
        settings.setFloatingBar(mode)
    }

    
    fun onShowNonCurrentWeekChanged(show: Boolean) {
        settings.setShowNonCurrentWeek(show)
    }

    
    fun onScheduleRowHeightChanged(heightDp: Int) {
        settings.setScheduleRowHeight(heightDp)
    }

    
    fun onReminderEnabledChanged(enabled: Boolean) {
        settings.setCourseReminderEnabled(enabled)
        if (!enabled) {
            viewModelScope.launch {
                withContext(AppIoDispatcher) {
                    runCatching { cancelAllCourseReminders() }
                }
            }
        }
    }

    
    fun onExamReminderEnabledChanged(enabled: Boolean) {
        settings.setExamReminderEnabled(enabled)
        if (!enabled) {
            viewModelScope.launch {
                withContext(AppIoDispatcher) {
                    runCatching { cancelAllExamReminders() }
                }
            }
        }
    }

    fun onLogEnabledChanged(enabled: Boolean) {
        settings.setLogEnabled(enabled)
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
            val avatarUri = settings.getUserAvatarUri()
            val nickname = settings.getUserNickname()
            _personalInfoUiState.value = PersonalInfoUiState(
                avatarUri = avatarUri,
                nickname = nickname,
                hasPersonalInfo = avatarUri != null || nickname != null,
            )
        }
    }
}
