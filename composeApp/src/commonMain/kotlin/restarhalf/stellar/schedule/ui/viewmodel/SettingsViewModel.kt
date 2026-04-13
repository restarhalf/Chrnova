package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val observeShowNonCurrentWeek: ObserveShowNonCurrentWeekUseCase,
    private val setShowNonCurrentWeekUseCase: SetShowNonCurrentWeekUseCase,
    private val observeCourseReminderEnabled: ObserveCourseReminderEnabledUseCase,
    private val setCourseReminderEnabled: SetCourseReminderEnabledUseCase,
    private val observeFloatingBar: ObserveFloatingBarUseCase,
    private val setFloatingBarUseCase: SetFloatingBarUseCase,
    private val cancelAllCourseReminders: CancelAllCourseRemindersUseCase,
    private val observeExamReminderEnabled: ObserveExamReminderEnabledUseCase,
    private val setExamReminderEnabled: SetExamReminderEnabledUseCase,
    private val cancelAllExamReminders: CancelAllExamRemindersUseCase,
    private val observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val observeSelectedTerm: ObserveSelectedTermUseCase,
    private val setSelectedTermUseCase: SetSelectedTermUseCase,
    private val observeAuthToken: ObserveAuthTokenUseCase,
    private val observeAuthProfile: ObserveAuthProfileUseCase,
    private val ensureLoggedIn: EnsureLoggedInUseCase,
    private val fetchSemesterIds: FetchSemesterIdsUseCase,
    private val scheduleNextCourseReminder: ScheduleNextCourseReminderUseCase,
    private val scheduleNextExamReminder: ScheduleNextExamReminderUseCase,
) : ViewModel() {

    data class ScreenUi(
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

    enum class NotificationTarget {
        None,
        Course,
        Exam,
    }

    private data class LoginSubmitPayload(
        val userNo: String,
        val password: String,
    )

    private companion object {
        val CAMPUS_OPTIONS = listOf("开发区", "金石滩")
        val THEME_OPTIONS = listOf("跟随系统", "浅色", "深色")
        val FLOATING_BAR_OPTIONS = listOf("固定", "悬浮","液态玻璃")
    }

    private val _showNonCurrentWeek = MutableStateFlow(true)
    val showNonCurrentWeek: StateFlow<Boolean> = _showNonCurrentWeek.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(false)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _examReminderEnabled = MutableStateFlow(false)
    val examReminderEnabled: StateFlow<Boolean> = _examReminderEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _floatingBar = MutableStateFlow(0)
    val floatingBar: StateFlow<Int> = _floatingBar.asStateFlow()

    private val _selectedTerm = MutableStateFlow("")
    val selectedTerm: StateFlow<String> = _selectedTerm.asStateFlow()

    private val _authToken = MutableStateFlow("")
    val authToken: StateFlow<String> = _authToken.asStateFlow()

    private val _profile = MutableStateFlow(AuthProfile())
    val profile: StateFlow<AuthProfile> = _profile.asStateFlow()

    private val _remoteTermItems = MutableStateFlow<List<String>>(emptyList())
    val remoteTermItems: StateFlow<List<String>> = _remoteTermItems.asStateFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _pendingNotificationTarget = MutableStateFlow(NotificationTarget.None)
    val pendingNotificationTarget: StateFlow<NotificationTarget> =
        _pendingNotificationTarget.asStateFlow()

    init {
        viewModelScope.launch {
            observeShowNonCurrentWeek().collect { _showNonCurrentWeek.value = it }
        }
        viewModelScope.launch {
            observeCourseReminderEnabled().collect {
                _reminderEnabled.value = it
            }
        }
        viewModelScope.launch {
            observeExamReminderEnabled().collect {
                _examReminderEnabled.value = it
            }
        }
        viewModelScope.launch { observeThemeMode().collect { _themeMode.value = it } }
        viewModelScope.launch { observeFloatingBar().collect { _floatingBar.value = it } }
        viewModelScope.launch { observeSelectedTerm().collect { _selectedTerm.value = it } }

        viewModelScope.launch { observeAuthToken().collect { _authToken.value = it } }
        viewModelScope.launch { observeAuthProfile().collect { _profile.value = it } }
    }

    fun refreshAuth() {
        if (_authToken.value.isBlank()) return

        viewModelScope.launch {
            withContext(AppIoDispatcher) {
                runCatching { ensureLoggedIn() }
            }
        }
    }

    fun refreshRemoteTerms() {
        if (_authToken.value.isBlank()) {
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
    ): ScreenUi {
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
        return ScreenUi(
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

    fun setSelectedTerm(value: String) {
        setSelectedTermUseCase.invoke(value)
    }

    fun showLoginSheet() {
        _loginUiState.value = _loginUiState.value.copy(showLoginSheet = true)
    }

    fun dismissLoginSheet() {
        _loginUiState.value =
            _loginUiState.value.copy(showLoginSheet = false, loading = false, error = "")
    }

    fun onLoginUserNoChange(value: String) {
        _loginUiState.value = _loginUiState.value.copy(userNo = value.trim(), error = "")
    }

    fun onLoginPasswordChange(value: String) {
        _loginUiState.value = _loginUiState.value.copy(password = value.trim(), error = "")
    }

    fun loginButtonEnabled(state: LoginUiState): Boolean {
        return !state.loading && state.userNo.isNotBlank() && state.password.isNotBlank()
    }

    fun loginHintMessage(state: LoginUiState): String {
        if (state.error.isNotBlank()) return ""

        val fields = buildList {
            if (state.userNo.containsCjkOrFullWidthChars()) add("账号")
            if (state.password.containsCjkOrFullWidthChars()) add("密码")
        }

        if (fields.isEmpty()) return ""
        return "${fields.joinToString("、")}里有中文字符"
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
        val current = _loginUiState.value
        if (current.loading) return
        val payload = validateLoginInput(current) ?: return

        _loginUiState.value =
            current.copy(
                userNo = payload.userNo,
                loading = true,
                error = "",
            )
        viewModelScope.launch {
            runCatching { onLogin(payload.userNo, payload.password) }
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

    private fun validateLoginInput(current: LoginUiState): LoginSubmitPayload? {
        val userNo = current.userNo.trim()
        val password = current.password.trim()
        val error =
            when {
                userNo.isBlank() -> "请输入账号"
                password.isBlank() -> "请输入密码"
                else -> null
            }

        if (error != null) {
            _loginUiState.value = current.copy(error = error)
            return null
        }

        return LoginSubmitPayload(userNo = userNo, password = password)
    }

    fun setThemeMode(mode: Int) {
        setThemeModeUseCase.invoke(mode)
    }

    fun setFloatingBar(mode: Int) {
        setFloatingBarUseCase.invoke(mode)
    }

    fun setShowNonCurrentWeek(show: Boolean) {
        setShowNonCurrentWeekUseCase.invoke(show)
    }

    fun setReminderEnabled(enabled: Boolean) {
        setCourseReminderEnabled.invoke(enabled)
        if (!enabled) {
            cancelAllCourseReminders()
        }
    }

    fun setExamReminderEnabled(enabled: Boolean) {
        setExamReminderEnabled.invoke(enabled)
        if (!enabled) {
            cancelAllExamReminders()
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
                setReminderEnabled(true)
                scheduleCourseReminder(
                    campus = campus,
                    termStartMs = termStartMs,
                    totalWeeks = totalWeeks
                )
            }

            NotificationTarget.Exam -> {
                setExamReminderEnabled(true)
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
                setReminderEnabled(false)
            }
        }
    }

    fun scheduleExamReminder(selectedTerm: String) {
        viewModelScope.launch {
            val result =
                withContext(AppIoDispatcher) {
                    runCatching { scheduleNextExamReminder(selectedTerm) }
                }
            if (result.isFailure && _authToken.value.isBlank()) {
                showLoginSheet()
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

private fun String.containsCjkOrFullWidthChars(): Boolean =
    any { char ->
        val code = char.code
        code in 0x2E80..0x9FFF ||
            code in 0xF900..0xFAFF ||
            code in 0xFF01..0xFF60 ||
            code in 0xFFE0..0xFFEE
    }
