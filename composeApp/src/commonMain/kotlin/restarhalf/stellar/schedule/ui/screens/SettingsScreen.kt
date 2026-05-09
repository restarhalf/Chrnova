package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.DatePickerBottomSheet
import restarhalf.stellar.schedule.ui.components.WeekPickerBottomSheet
import restarhalf.stellar.schedule.ui.icons.Logout
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class SettingsScreenState(
    val syncUiState: SyncUiState,
    val campus: Campus,
    val termStartMs: Long,
    val totalWeeks: Int,
)

data class SettingsScreenActions(
    val onSync: suspend () -> Unit,
    val onLogout: () -> Unit,
    val onLogin: suspend (userNo: String, password: String) -> Unit,
    val ensureCourseReminderPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
    val ensureExamReminderPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
    val onCampusChange: (Campus) -> Unit,
    val onTermStartChange: (Long) -> Unit,
    val onTotalWeeksChange: (Int) -> Unit,
    val onChangeBackground: () -> Unit,
    val onAbout: () -> Unit,
)

@OptIn(ExperimentalTime::class)
@Composable
fun SettingsScreen(
    state: SettingsScreenState,
    actions: SettingsScreenActions,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val vm: SettingsViewModel = koinViewModel()
    val overscrollEffect = MiuixOverscrollEffect()

    val settingsUiState by vm.uiState.collectAsState()

    val scope = rememberCoroutineScope()

    val screenUi =
        remember(state.syncUiState, state.campus, state.termStartMs) {
            vm.buildScreenUi(
                syncUiState = state.syncUiState,
                campus = state.campus,
                termStartMs = state.termStartMs
            )
        }

    val showTermStartPicker = remember { mutableStateOf(false) }
    val showTotalWeeksPicker = remember { mutableStateOf(false) }

    LaunchedEffect(settingsUiState.loginUiState.authVersion) {
        vm.refreshAuth()
        vm.refreshRemoteTerms()
    }

    val termSelectionUi =
        remember(
            settingsUiState.remoteTermItems,
            settingsUiState.authToken,
            settingsUiState.selectedTerm,
        ) {
            vm.buildTermSelectionUi(
                authToken = settingsUiState.authToken,
                remoteTermItems = settingsUiState.remoteTermItems,
                selectedTerm = settingsUiState.selectedTerm
            )
        }
    val accountUi =
        remember(settingsUiState.authToken, settingsUiState.profile) {
            vm.buildAccountUi(settingsUiState.authToken, settingsUiState.profile)
        }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(title = "课程表设置", scrollBehavior = topAppBarScrollBehavior)
        },
        popupHost = {
            if (settingsUiState.loginUiState.showLoginSheet) {
                OverlayDialog(
                    show = settingsUiState.loginUiState.showLoginSheet,
                    modifier = Modifier,
                    title = "登录",
                    titleColor = DialogDefaults.titleColor(),
                    summary = null,
                    summaryColor = DialogDefaults.summaryColor(),
                    backgroundColor = DialogDefaults.backgroundColor(),
                    enableWindowDim = true,
                    onDismissRequest = {
                        vm.dismissLoginSheet()
                    },
                    onDismissFinished = null,
                    outsideMargin = DialogDefaults.outsideMargin,
                    insideMargin = DialogDefaults.insideMargin,
                    defaultWindowInsetsPadding = true,
                    renderInRootScaffold = true,
                    content = {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Column {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextField(
                                        label = "账号",
                                        value = settingsUiState.loginUiState.userNo,
                                        onValueChange = {
                                            vm.onLoginUserNoChange(it)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            item {
                                Column {
                                    TextField(
                                        label = "密码",
                                        value = settingsUiState.loginUiState.password,
                                        onValueChange = {
                                            vm.onLoginPasswordChange(it)
                                        },
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            if (settingsUiState.loginUiState.error.isNotBlank()) {
                                item { Text(text = settingsUiState.loginUiState.error) }
                            }
                            item {
                                Button(
                                    enabled =
                                        !settingsUiState.loginUiState.loading &&
                                                settingsUiState.loginUiState.userNo.isNotBlank() &&
                                                settingsUiState.loginUiState.password.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    onClick = {
                                        vm.submitLogin(actions.onLogin)
                                    }) {
                                    Text(
                                        text =
                                            if (settingsUiState.loginUiState.loading) "登录中..."
                                            else "登录",
                                        color = MiuixTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    })
            }

            if (settingsUiState.loginUiState.showLogoutConfirm) {
                OverlayDialog(
                    show = settingsUiState.loginUiState.showLogoutConfirm,
                    modifier = Modifier,
                    title = "确认退出登录",
                    titleColor = DialogDefaults.titleColor(),
                    summary = null,
                    summaryColor = DialogDefaults.summaryColor(),
                    backgroundColor = DialogDefaults.backgroundColor(),
                    enableWindowDim = true,
                    onDismissRequest = { vm.dismissLogoutConfirm() },
                    onDismissFinished = null,
                    outsideMargin = DialogDefaults.outsideMargin,
                    insideMargin = DialogDefaults.insideMargin,
                    defaultWindowInsetsPadding = true,
                    renderInRootScaffold = true,
                    content = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { vm.dismissLogoutConfirm() }) {
                                Text(text = "取消")
                            }

                            Spacer(modifier = Modifier.size(16.dp))
                            Button(
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                onClick = {
                                    vm.confirmLogout(actions.onLogout)
                                }) {
                                Text(text = "确认", color = MiuixTheme.colorScheme.onPrimary)
                            }
                        }
                    })
            }
            if (showTermStartPicker.value) {
                DatePickerBottomSheet(
                    show = showTermStartPicker,
                    title = "选择开始上课时间",
                    initialDate =
                        Instant.fromEpochMilliseconds(state.termStartMs)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date,
                    onConfirm = { date: LocalDate ->
                        val ms = date.atStartOfDayIn(TimeZone.currentSystemDefault())
                            .toEpochMilliseconds()
                        actions.onTermStartChange(ms)
                        showTermStartPicker.value = false
                    },
                )
            }
            if (showTotalWeeksPicker.value) {
                WeekPickerBottomSheet(
                    show = showTotalWeeksPicker,
                    title = "本学期总周数",
                    initialWeek = state.totalWeeks,
                    weekRange = 1..20,
                    onConfirm = { week: Int ->
                        actions.onTotalWeeksChange(week)
                        showTotalWeeksPicker.value = false
                    },
                )
            }
        }) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            start =
                                paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = 0.dp
                        )
                    )
                    .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding =
                appPageContentPadding(
                    innerPadding = PaddingValues(),
                    outerPadding = appScaffoldPadding,
                    extraTop = 12.dp,
                    extraStart = 16.dp,
                    extraEnd = 16.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            overscrollEffect = overscrollEffect
        ) {
            item {
                SmallTitle(text = "账号")
                if (accountUi.loggedIn) {
                    AppCard {
                        BasicComponent(
                            title = accountUi.title,
                            summary = accountUi.summary,
                            endActions = {
                                IconButton(onClick = { vm.requestLogoutConfirm() }) {
                                    Icon(
                                        imageVector = Logout,
                                        contentDescription = "退出",
                                    )
                                }
                            })
                    }
                } else {
                    AppCard {
                        ArrowPreference(
                            title = "登录",
                            summary = "用于获取课表",
                            onClick = { vm.showLoginSheet() })
                    }
                }
            }
            item {
                SmallTitle(text = "基本设置")
                AppCard {
                    OverlayDropdownPreference(
                        title = "学期",
                        summary = "用于查询课表和成绩",
                        items = termSelectionUi.items,
                        selectedIndex = termSelectionUi.selectedIndex,
                        onSelectedIndexChange = { index: Int ->
                            vm.onSelectedTermChanged(
                                vm.selectedTermValueFromIndex(termSelectionUi.items, index)
                            )
                            scope.launch { runCatching { actions.onSync() } }
                        })
                    OverlayDropdownPreference(
                        title = "上课校区",
                        summary = "用于课表时间与作息展示",
                        items = screenUi.campusOptions,
                        selectedIndex = screenUi.campusSelectedIndex,
                        onSelectedIndexChange = { index: Int ->
                            actions.onCampusChange(vm.campusFromIndex(index))
                            scope.launch { runCatching { actions.onSync() } }
                        })
                    ArrowPreference(
                        title = "开始上课时间",
                        summary = screenUi.termStartSummary,
                        onClick = { showTermStartPicker.value = true })

                    ArrowPreference(
                        title = "本学期总周数",
                        summary = state.totalWeeks.toString(),
                        onClick = { showTotalWeeksPicker.value = true })
                    SwitchPreference(
                        title = "是否显示非本周课程",
                        summary = "开启后单双周课程都可以看见哦",
                        checked = settingsUiState.showNonCurrentWeek,
                        onCheckedChange = {
                            vm.onShowNonCurrentWeekChanged(it)
                        })
                }
            }

            item {
                SmallTitle(text = "外观")
                AppCard {
                    OverlayDropdownPreference(
                        title = "主题模式",
                        summary = "深色/浅色可跟随系统",
                        items = screenUi.themeOptions,
                        selectedIndex = settingsUiState.themeMode.coerceIn(0, 2),
                        onSelectedIndexChange = { index: Int ->
                            vm.onThemeModeChanged(index)
                        })
                    OverlayDropdownPreference(
                        title = "底栏形式",
                        summary = "选择底栏的状态",
                        items = screenUi.floatingBarOptions,
                        selectedIndex = settingsUiState.floatingBar.coerceIn(0, 2),
                        onSelectedIndexChange = { index: Int ->
                            vm.onFloatingBarChanged(index)
                        }
                    )
                    ArrowPreference(
                        title = "更换背景",
                        summary = "设置背景图片、模糊度与透明度",
                        onClick = actions.onChangeBackground
                    )
                }
            }

            item {
                SmallTitle(text = "杂项")
                AppCard {
                    ArrowPreference(
                        title = "手动刷新课表",
                        summary = screenUi.syncSummary,
                        onClick = { scope.launch { runCatching { actions.onSync() } } })
                    SwitchPreference(
                        title = "课程提醒",
                        summary = "上课前15分钟推送通知提醒",
                        checked = settingsUiState.reminderEnabled,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                actions.ensureCourseReminderPermission {
                                    vm.onReminderEnabledChanged(true)
                                    vm.scheduleCourseReminder(
                                        campus = state.campus,
                                        termStartMs = state.termStartMs,
                                        totalWeeks = state.totalWeeks
                                    )
                                }
                            } else {
                                vm.onReminderEnabledChanged(false)
                            }
                        })
                    SwitchPreference(
                        title = "考试提醒",
                        summary = "考试前15分钟推送通知提醒",
                        checked = settingsUiState.examReminderEnabled,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                actions.ensureExamReminderPermission {
                                    vm.onExamReminderEnabledChanged(true)
                                    vm.scheduleExamReminder(
                                        selectedTerm = settingsUiState.selectedTerm
                                    )
                                }
                            } else {
                                vm.onExamReminderEnabledChanged(false)
                            }
                        })
                }
            }
            item {
                SmallTitle(text = "关于")
                AppCard {
                    ArrowPreference(
                        title = "关于",
                        summary = "版本信息及更新",
                        onClick = actions.onAbout
                    )
                }
            }
        }
    }
}
