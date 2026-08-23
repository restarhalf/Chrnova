package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.text.CsvExporter
import restarhalf.stellar.schedule.core.text.IcsExporter
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.CardItem
import restarhalf.stellar.schedule.ui.components.DatePickerBottomSheet
import restarhalf.stellar.schedule.ui.components.PersonalInfoCard
import restarhalf.stellar.schedule.ui.components.StarVerificationDialog
import restarhalf.stellar.schedule.ui.components.WeekPickerBottomSheet
import restarhalf.stellar.schedule.ui.components.groupedCardItems
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import org.koin.compose.koinInject
import restarhalf.stellar.schedule.config.LocalSecrets
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 设置页面屏幕
 *
 * 显示应用设置，包括：
 * - 账号登录/登出
 * - 校区选择
 * - 学期设置
 * - 提醒设置
 * - 主题设置
 * - 背景设置
 * - 关于页面
 *
 * @param syncUiState 同步状态
 * @param campus 当前校区
 * @param termStartMs 学期开始时间戳
 * @param totalWeeks 学期总周数
 * @param onSync 同步回调
 * @param onLogout 登出回调
 * @param onLogin 登录回调
 * @param ensureCourseReminderPermission 课程提醒权限请求回调
 * @param ensureExamReminderPermission 考试提醒权限请求回调
 * @param onCampusChange 校区变更回调
 * @param onTermStartChange 学期开始时间变更回调
 * @param onTotalWeeksChange 总周数变更回调
 * @param onChangeBackground 更换背景回调
 * @param onAbout 关于页面回调
 */
@OptIn(ExperimentalTime::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    syncUiState: SyncUiState,
    campus: Campus,
    termStartMs: Long,
    totalWeeks: Int,
    onSync: suspend () -> Unit,
    onLogout: () -> Unit,
    onLogin: () -> Unit,
    isPeLoggedIn: Boolean = false,
    ensureCourseReminderPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
    ensureExamReminderPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
    onCampusChange: (Campus) -> Unit,
    onTermStartChange: (Long) -> Unit,
    onTotalWeeksChange: (Int) -> Unit,
    onChangeBackground: () -> Unit,
    onAbout: () -> Unit,
    onPaper: () -> Unit,
    onEvaluation: () -> Unit = {},
    onProfile: () -> Unit = {},
    onFoodRoulette: () -> Unit = {},
    onCourseSelection: () -> Unit = {},
    onExportCsv: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    courses: List<Course> = emptyList(),
    showMessage: (String) -> Unit = {},
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val settingsUiState by vm.uiState.collectAsStateWithLifecycle()
    val timetablePort: TimetablePort = koinInject()

    val screenUi =
        remember(syncUiState, campus, termStartMs) {
            vm.buildScreenUi(
                syncUiState = syncUiState,
                campus = campus,
                termStartMs = termStartMs,
            )
        }

    val showTermStartPicker = remember { mutableStateOf(false) }
    val showTotalWeeksPicker = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val starVerificationState by vm.starVerification.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
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

    val personalInfoUiState by vm.personalInfoUiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.loadPersonalInfo()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(title = "课程表设置", scrollBehavior = topAppBarScrollBehavior)
        },
        popupHost = {
            if (showTermStartPicker.value) {
                DatePickerBottomSheet(
                    show = showTermStartPicker.value,
                    title = "选择开始上课时间",
                    initialDate =
                        Instant.fromEpochMilliseconds(termStartMs)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date,
                    onDismissRequest = { showTermStartPicker.value = false },
                    onConfirm = { date: LocalDate ->
                        val ms = date.atStartOfDayIn(TimeZone.currentSystemDefault())
                            .toEpochMilliseconds()
                        onTermStartChange(ms)
                        showTermStartPicker.value = false
                    },
                )
            }
            if (showTotalWeeksPicker.value) {
                WeekPickerBottomSheet(
                    show = showTotalWeeksPicker.value,
                    title = "本学期总周数",
                    initialWeek = totalWeeks,
                    weekRange = 1..20,
                    onDismissRequest = { showTotalWeeksPicker.value = false },
                    onConfirm = { week: Int ->
                        onTotalWeeksChange(week)
                        showTotalWeeksPicker.value = false
                    },
                )
            }
            StarVerificationDialog(
                show = starVerificationState.showDialog,
                username = starVerificationState.username,
                isVerifying = starVerificationState.isVerifying,
                error = starVerificationState.error,
                onUsernameChange = { vm.starVerification.onUsernameChange(it) },
                onVerify = { vm.starVerification.verify() },
                onDismiss = { vm.starVerification.dismissDialog() },
            )
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
                    extraStart = 12.dp,
                    extraEnd = 12.dp,
                ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            item {
                SmallTitle(text = "账号")
                AppCard {
                    // 个人信息卡（头像/昵称）仅用于展示与编辑，不应影响登录入口的可用性
                    if (personalInfoUiState.hasPersonalInfo) {
                        PersonalInfoCard(
                            avatarUri = personalInfoUiState.avatarUri,
                            nickname = personalInfoUiState.nickname ?: settingsUiState.profile.name,
                            academyName = settingsUiState.profile.academyName,
                            onClick = onProfile,
                        )
                    } else if (accountUi.loggedIn) {
                        BasicComponent(
                            title = accountUi.title,
                            summary = accountUi.summary,
                            onClick = onProfile,
                        )
                    }
                    // 登录入口：只要未登录教务就始终显示，与是否设置头像/昵称无关
                    if (!accountUi.loggedIn) {
                        if (personalInfoUiState.hasPersonalInfo) {
                            HorizontalDivider()
                        }
                        ArrowPreference(
                            title = "登录教务系统",
                            summary = "用于获取课表",
                            onClick = { onLogin() })
                    }
                    if (!accountUi.loggedIn && isPeLoggedIn) {
                        HorizontalDivider()
                        Box(modifier = Modifier.fillMaxSize())
                        {
                            ArrowPreference(
                                title = "账号管理",
                                summary = "管理体测平台账号",
                                onClick = onProfile,
                            )
                        }
                    }
                }
            }
            item {
                SmallTitle(text = "基本设置")
            }
            groupedCardItems(
                keyPrefix = "settings_basic",
                outerBottomPadding = 6.dp,
                outerHorizontalPadding = 0.dp,
                items = listOf(
                    CardItem("term") {
                        OverlayDropdownPreference(
                            title = "学期",
                            summary = "用于查询课表和成绩",
                            items = termSelectionUi.items,
                            selectedIndex = termSelectionUi.selectedIndex,
                            onSelectedIndexChange = { index: Int ->
                                vm.onSelectedTermChanged(
                                    vm.selectedTermValueFromIndex(termSelectionUi.items, index)
                                )
                                vm.triggerSync { onSync() }
                            })
                    },
                    CardItem("campus") {
                        OverlayDropdownPreference(
                            title = "上课校区",
                            summary = "用于课表时间与作息展示",
                            items = screenUi.campusOptions,
                            selectedIndex = screenUi.campusSelectedIndex,
                            onSelectedIndexChange = { index: Int ->
                                onCampusChange(vm.campusFromIndex(index))
                                vm.triggerSync { onSync() }
                            })
                    },
                    CardItem("termStart") {
                        ArrowPreference(
                            title = "开始上课时间",
                            summary = screenUi.termStartSummary,
                            onClick = { showTermStartPicker.value = true })
                    },
                    CardItem("totalWeeks") {
                        ArrowPreference(
                            title = "本学期总周数",
                            summary = totalWeeks.toString(),
                            onClick = { showTotalWeeksPicker.value = true })
                    },
                    CardItem("showNonCurrentWeek") {
                        SwitchPreference(
                            title = "是否显示非本周课程",
                            summary = "开启后单双周课程都可以看见哦",
                            checked = settingsUiState.showNonCurrentWeek,
                            onCheckedChange = {
                                vm.onShowNonCurrentWeekChanged(it)
                            })
                    },
                ),
            )

            item {
                SmallTitle(text = "小工具")
            }
            groupedCardItems(
                keyPrefix = "settings_tools",
                outerBottomPadding = 6.dp,
                outerHorizontalPadding = 0.dp,
                items = buildList{
                    add(CardItem("paper"){
                        ArrowPreference(
                            title = "试卷共享",
                            summary = "校园试卷与学习资料共享",
                            onClick = onPaper,
                        )
                    })
                    add(CardItem("evaluation") {
                        ArrowPreference(
                            title = "课程评价",
                            summary = "查看与分享课程评价",
                            onClick = onEvaluation,
                        )
                    })
//                        add(CardItem("courseSelection") {
//                            ArrowPreference(
//                                title = "自动抢课",
//                                summary = "自动监控并提交选课请求",
//                                onClick = onCourseSelection,
//                            )
//                        })
                    add(CardItem("exportCsv") {
                        ArrowPreference(
                            title = "导出课表CSV",
                            summary = "将当前课表导出为CSV文件",
                            onClick = {
                                if (!starVerificationState.isVerified) {
                                    vm.starVerification.showDialog()
                                } else {
                                    scope.launch {
                                        val csv = CsvExporter.export(courses)
                                        val path = onExportCsv("课表.csv", csv)
                                        if (path != null) {
                                            showMessage("已保存: $path")
                                        } else {
                                            showMessage("保存失败")
                                        }
                                    }
                                }
                            })
                    })
                    add(CardItem("exportIcs") {
                        ArrowPreference(
                            title = "导出日历ICS",
                            summary = "导入系统日历,支持课前提醒",
                            onClick = {
                                if (!starVerificationState.isVerified) {
                                    vm.starVerification.showDialog()
                                } else {
                                    scope.launch {
                                        val timetable = timetablePort.getCampusTimetable(campus)
                                        val ics = IcsExporter.export(courses, termStartMs, timetable)
                                        val path = onExportCsv("课表.ics", ics)
                                        if (path != null) {
                                            showMessage("已保存: $path")
                                        } else {
                                            showMessage("保存失败")
                                        }
                                    }
                                }
                            })
                    })
//                    add(CardItem("foodRoulette") {
//                        ArrowPreference(
//                            title = "今天吃什么",
//                            summary = "选择困难症？让滚轮帮你决定",
//                            onClick = onFoodRoulette,
//                        )
//                    })
                }
            )


            item {
                SmallTitle(text = "外观")
            }
            groupedCardItems(
                keyPrefix = "settings_appearance",
                outerBottomPadding = 6.dp,
                outerHorizontalPadding = 0.dp,
                items = listOf(
                    CardItem("theme") {
                        OverlayDropdownPreference(
                            title = "主题模式",
                            summary = "深色/浅色可跟随系统",
                            items = screenUi.themeOptions,
                            selectedIndex = settingsUiState.themeMode.coerceIn(0, 2),
                            onSelectedIndexChange = { index: Int ->
                                vm.onThemeModeChanged(index)
                            })
                    },
                    CardItem("floatingBar") {
                        OverlayDropdownPreference(
                            title = "底栏形式",
                            summary = "选择底栏的状态",
                            items = screenUi.floatingBarOptions,
                            selectedIndex = settingsUiState.floatingBar.coerceIn(0, 2),
                            onSelectedIndexChange = { index: Int ->
                                vm.onFloatingBarChanged(index)
                            }
                        )
                    },
                    CardItem("background") {
                        ArrowPreference(
                            title = "更换背景",
                            summary = "设置背景图片、模糊度与透明度",
                            onClick = onChangeBackground
                        )
                    },
                ),
            )

            item {
                SmallTitle(text = "杂项")
            }
            groupedCardItems(
                keyPrefix = "settings_misc",
                outerBottomPadding = 6.dp,
                outerHorizontalPadding = 0.dp,
                items = buildList {
                    add(CardItem("sync") {
                        ArrowPreference(
                            title = "手动刷新课表",
                            summary = screenUi.syncSummary,
                            onClick = { vm.triggerSync { onSync() } })
                    })
                    add(CardItem("courseReminder") {
                        SwitchPreference(
                            title = "课程日历提醒",
                            summary = "写入系统日历,课前15分钟提醒",
                            checked = settingsUiState.reminderEnabled,
                            onCheckedChange = { newValue ->
                                if (newValue) {
                                    ensureCourseReminderPermission {
                                        vm.onReminderEnabledChanged(true)
                                        vm.syncCourseCalendar(
                                            campus = campus,
                                            termStartMs = termStartMs,
                                            totalWeeks = totalWeeks
                                        )
                                    }
                                } else {
                                    vm.onReminderEnabledChanged(false)
                                }
                            })
                    })
                    add(CardItem("examReminder") {
                        SwitchPreference(
                            title = "考试日历提醒",
                            summary = "写入系统日历,考前15分钟提醒",
                            checked = settingsUiState.examReminderEnabled,
                            onCheckedChange = { newValue ->
                                if (newValue) {
                                    ensureExamReminderPermission {
                                        vm.onExamReminderEnabledChanged(true)
                                        vm.syncExamCalendar(
                                            selectedTerm = settingsUiState.selectedTerm
                                        )
                                    }
                                } else {
                                    vm.onExamReminderEnabledChanged(false)
                                }
                            })
                    })
                    add(CardItem("log") {
                        SwitchPreference(
                            title = "开启日志",
                            summary = "关于页面连续点击五次应用版本进入日志",
                            checked = settingsUiState.logEnabled,
                            onCheckedChange = {
                                vm.onLogEnabledChanged(it)
                            })
                    })
                },
            )
            item {
                SmallTitle(text = "关于")
                AppCard {
                    ArrowPreference(
                        title = "关于",
                        summary = "版本信息及更新",
                        onClick = onAbout
                    )
                }
            }
        }
    }
}
