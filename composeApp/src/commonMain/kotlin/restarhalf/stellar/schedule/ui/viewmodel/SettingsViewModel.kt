package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.domain.model.AuthProfile
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.usecase.CancelAllCourseRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllExamRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.EnsureLoggedInUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAuthProfileUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAuthTokenUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveCourseReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveExamReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveFloatingBarUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveSelectedTermUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveShowNonCurrentWeekUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveThemeModeUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextCourseReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextExamReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.SetCourseReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.SetExamReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.SetFloatingBarUseCase
import restarhalf.stellar.schedule.domain.usecase.SetSelectedTermUseCase
import restarhalf.stellar.schedule.domain.usecase.SetShowNonCurrentWeekUseCase
import restarhalf.stellar.schedule.domain.usecase.SetThemeModeUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class SettingsViewModel(
    observeAuthToken: ObserveAuthTokenUseCase,
    observeAuthProfile: ObserveAuthProfileUseCase,
    observeShowNonCurrentWeek: ObserveShowNonCurrentWeekUseCase,
    observeCourseReminderEnabled: ObserveCourseReminderEnabledUseCase,
    observeFloatingBar: ObserveFloatingBarUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    observeExamReminderEnabled: ObserveExamReminderEnabledUseCase,
    observeSelectedTerm: ObserveSelectedTermUseCase,
    private val setShowNonCurrentWeekUseCase: SetShowNonCurrentWeekUseCase,
    private val setCourseReminderEnabled: SetCourseReminderEnabledUseCase,
    private val setFloatingBarUseCase: SetFloatingBarUseCase,
    private val cancelAllCourseReminders: CancelAllCourseRemindersUseCase,
    private val setExamReminderEnabled: SetExamReminderEnabledUseCase,
    private val cancelAllExamReminders: CancelAllExamRemindersUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setSelectedTermUseCase: SetSelectedTermUseCase,
    private val ensureLoggedIn: EnsureLoggedInUseCase,
    private val fetchSemesterIds: FetchSemesterIdsUseCase,
    private val scheduleNextCourseReminder: ScheduleNextCourseReminderUseCase,
    private val scheduleNextExamReminder: ScheduleNextExamReminderUseCase,
) : ViewModel() {
    private data class SettingsPrefsState(
        val showNonCurrentWeek: Boolean,
        val reminderEnabled: Boolean,
        val examReminderEnabled: Boolean,
        val themeMode: Int,
        val floatingBar: Int,
        val selectedTerm: String,
    )

    private data class SettingsAuthState(
        val authToken: String,
        val profile: AuthProfile,
    )

    data class SettingsScreenUi(
        val campusOptions: List<String>,
        val campusSelectedIndex: Int,
        val themeOptions: List<String>,
        val floatingBarOptions: List<String>,
        val termStartSummary: String,
        val syncSummary: String,
    )

    data class TermSelectionUi(
        val items: List<String>,
        val selectedIndex: Int,
    )

    data class AccountUi(
        val loggedIn: Boolean,
        val title: String,
        val summary: String,
    )

    data class LoginUiState(
        val showLoginSheet: Boolean = false,
        val showLogoutConfirm: Boolean = false,
        val userNo: String = "",
        val password: String = "",
        val error: String = "",
        val loading: Boolean = false,
        val authVersion: Int = 0,
    )

    data class SettingsUiState(
        val showNonCurrentWeek: Boolean,
        val reminderEnabled: Boolean,
        val examReminderEnabled: Boolean,
        val themeMode: Int,
        val floatingBar: Int,
        val selectedTerm: String,
        val authToken: String,
        val profile: AuthProfile,
        val remoteTermItems: List<String>,
        val loginUiState: LoginUiState,
        val pendingNotificationTarget: NotificationTarget,
    )

    enum class NotificationTarget {
        None,
        Course,
        Exam,
    }

    private companion object {
        val CAMPUS_OPTIONS = listOf("开发区", "金石滩")
        val THEME_OPTIONS = listOf("跟随系统", "浅色", "深色")
        val FLOATING_BAR_OPTIONS = listOf("固定", "悬浮","液态玻璃")
    }

    private val _remoteTermItems = MutableStateFlow<List<String>>(emptyList())
    private val _loginUiState = MutableStateFlow(LoginUiState())
    private val _pendingNotificationTarget = MutableStateFlow(NotificationTarget.None)
    private val loginMutex = Mutex()

    private val prefsFlow =
        combine(
            observeShowNonCurrentWeek(),
            observeCourseReminderEnabled(),
            observeExamReminderEnabled(),
            observeThemeMode(),
            observeFloatingBar(),
        ) { showNonCurrentWeek, reminderEnabled, examReminderEnabled, themeMode, floatingBar ->
            SettingsPrefsState(
                showNonCurrentWeek = showNonCurrentWeek,
                reminderEnabled = reminderEnabled,
                examReminderEnabled = examReminderEnabled,
                themeMode = themeMode,
                floatingBar = floatingBar,
                selectedTerm = "",
            )
        }
            .combine(observeSelectedTerm()) { prefs, selectedTerm ->
                prefs.copy(selectedTerm = selectedTerm)
            }

    private val authFlow =
        combine(observeAuthToken(), observeAuthProfile()) { authToken, profile ->
            SettingsAuthState(authToken = authToken, profile = profile)
        }

    private val _uiState: StateFlow<SettingsUiState> =
        prefsFlow
            .combine(authFlow) { prefs, auth -> prefs to auth }
            .combine(_remoteTermItems) { prefsAuth, remoteTermItems ->
                Triple(prefsAuth.first, prefsAuth.second, remoteTermItems)
            }
            .combine(_loginUiState) { prefsAuthRemote, loginUiState ->
                prefsAuthRemote to loginUiState
            }
            .combine(_pendingNotificationTarget) { prefsBundle, pendingNotificationTarget ->
                val (prefsAuthRemote, loginUiState) = prefsBundle
                val (prefs, auth, remoteTermItems) = prefsAuthRemote
                SettingsUiState(
                    showNonCurrentWeek = prefs.showNonCurrentWeek,
                    reminderEnabled = prefs.reminderEnabled,
                    examReminderEnabled = prefs.examReminderEnabled,
                    themeMode = prefs.themeMode,
                    floatingBar = prefs.floatingBar,
                    selectedTerm = prefs.selectedTerm,
                    authToken = auth.authToken,
                    profile = auth.profile,
                    remoteTermItems = remoteTermItems,
                    loginUiState = loginUiState,
                    pendingNotificationTarget = pendingNotificationTarget,
                )
            }
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
                        authToken = "",
                        profile = AuthProfile(),
                        remoteTermItems = emptyList(),
                        loginUiState = LoginUiState(),
                        pendingNotificationTarget = NotificationTarget.None,
                    ),
            )

    val uiState: StateFlow<SettingsUiState> = _uiState

    fun refreshAuth() {
        if (uiState.value.authToken.isBlank()) return

        viewModelScope.launch {
            withContext(AppIoDispatcher) {
                runCatching { ensureLoggedIn() }
            }
        }
    }

    fun refreshRemoteTerms() {
        if (uiState.value.authToken.isBlank()) {
            _remoteTermItems.value = emptyList()
            return
        }

        viewModelScope.launch {
            val terms =
                withContext(AppIoDispatcher) {
                    fetchSemesterIds()
                }
            _remoteTermItems.value = terms
        }
    }

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

    fun selectedTermValueFromIndex(items: List<String>, index: Int): String {
        val value = items.getOrNull(index).orEmpty()
        return if (value == "当前学期") "" else value
    }

    fun campusFromIndex(index: Int): Campus {
        return if (index == 0) Campus.Development else Campus.Jinshitan
    }

    fun onSelectedTermChanged(value: String) {
        setSelectedTermUseCase.invoke(value)
    }

    fun onThemeModeChanged(mode: Int) {
        setThemeModeUseCase.invoke(mode)
    }

    fun onFloatingBarChanged(mode: Int) {
        setFloatingBarUseCase.invoke(mode)
    }

    fun onShowNonCurrentWeekChanged(show: Boolean) {
        setShowNonCurrentWeekUseCase.invoke(show)
    }

    fun onReminderEnabledChanged(enabled: Boolean) {
        setCourseReminderEnabled.invoke(enabled)
        if (!enabled) {
            cancelAllCourseReminders()
        }
    }

    fun onExamReminderEnabledChanged(enabled: Boolean) {
        setExamReminderEnabled.invoke(enabled)
        if (!enabled) {
            cancelAllExamReminders()
        }
    }

    fun showLoginSheet() {
        _loginUiState.value = _loginUiState.value.copy(showLoginSheet = true)
    }

    fun dismissLoginSheet() {
        _loginUiState.value =
            _loginUiState.value.copy(showLoginSheet = false, loading = false, error = "")
    }

    fun onLoginUserNoChange(value: String) {
        _loginUiState.value = _loginUiState.value.copy(userNo = value, error = "")
    }

    fun onLoginPasswordChange(value: String) {
        _loginUiState.value = _loginUiState.value.copy(password = value, error = "")
    }

    fun requestLogoutConfirm() {
        _loginUiState.value = _loginUiState.value.copy(showLogoutConfirm = true)
    }

    fun dismissLogoutConfirm() {
        _loginUiState.value = _loginUiState.value.copy(showLogoutConfirm = false)
    }

    fun confirmLogout(onLogout: () -> Unit) {
        _loginUiState.value = _loginUiState.value.copy(showLogoutConfirm = false)
        onLogout()
        _loginUiState.value =
            _loginUiState.value.copy(authVersion = _loginUiState.value.authVersion + 1)
    }

    fun submitLogin(onLogin: suspend (userNo: String, password: String) -> Unit) {
        viewModelScope.launch {
            loginMutex.withLock {
                val current = _loginUiState.value
                if (current.loading) return@withLock
                val userNo = current.userNo.trim()
                val password = current.password
                if (userNo.isBlank() || password.isBlank()) return@withLock

                _loginUiState.value = current.copy(loading = true, error = "")
                runCatching { onLogin(userNo, password) }
                    .onSuccess {
                        val latest = _loginUiState.value
                        _loginUiState.value =
                            latest.copy(
                                showLoginSheet = false,
                                password = "",
                                loading = false,
                                authVersion = latest.authVersion + 1,
                            )
                    }
                    .onFailure {
                        val latest = _loginUiState.value
                        _loginUiState.value =
                            latest.copy(
                                loading = false,
                                error = it.toUserFacingMessage(UserFacingErrorKind.Login)
                            )
                    }
            }
        }
    }

    fun requestNotificationPermission(target: NotificationTarget) {
        _pendingNotificationTarget.value = target
    }

    fun clearPendingNotificationTarget() {
        _pendingNotificationTarget.value = NotificationTarget.None
    }

    fun handleNotificationPermissionResult(
        granted: Boolean,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        selectedTerm: String,
    ) {
        val target = _pendingNotificationTarget.value
        _pendingNotificationTarget.value = NotificationTarget.None
        if (!granted) return
        when (target) {
            NotificationTarget.Course -> {
                onReminderEnabledChanged(true)
                scheduleCourseReminder(
                    campus = campus,
                    termStartMs = termStartMs,
                    totalWeeks = totalWeeks
                )
            }

            NotificationTarget.Exam -> {
                onExamReminderEnabledChanged(true)
                scheduleExamReminder(selectedTerm = selectedTerm)
            }

            NotificationTarget.None -> Unit
        }
    }

    fun scheduleCourseReminder(campus: Campus, termStartMs: Long, totalWeeks: Int) {
        viewModelScope.launch {
            val success =
                withContext(AppIoDispatcher) {
                    runCatching {
                        scheduleNextCourseReminder(
                            campus = campus,
                            termStartMs = termStartMs,
                            totalWeeks = totalWeeks
                        )
                    }.isSuccess
                }
            if (!success) {
                onReminderEnabledChanged(false)
            }
        }
    }

    fun scheduleExamReminder(selectedTerm: String) {
        viewModelScope.launch {
            val success =
                withContext(AppIoDispatcher) {
                    runCatching {
                        scheduleNextExamReminder(selectedTerm = selectedTerm)
                    }.isSuccess
                }
            if (!success) {
                onExamReminderEnabledChanged(false)
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
}
