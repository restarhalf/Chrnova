package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentList
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.ExactAlarmPermissionState
import restarhalf.stellar.schedule.domain.reminder.ClassroomLabel
import restarhalf.stellar.schedule.domain.reminder.ClassroomLabelRule
import restarhalf.stellar.schedule.domain.reminder.ReminderTiming
import restarhalf.stellar.schedule.domain.usecase.ClassroomLabelPreview
import restarhalf.stellar.schedule.ui.chrome.AppPageScaffold
import restarhalf.stellar.schedule.ui.chrome.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.GlassDropdownMenu
import restarhalf.stellar.schedule.ui.components.LocalLiquidGlassBackdrop
import restarhalf.stellar.schedule.ui.components.WheelPickerBottomSheet
import restarhalf.stellar.schedule.ui.components.WheelPickerColumnState
import restarhalf.stellar.schedule.ui.components.WheelPickerRow
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.overlay.LocalSheetContentBackdrop
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.layout.collapsibleTopInset
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private const val DND_MODE_DND = 0
private const val DND_MODE_SILENT = 1
private const val DND_MODE_PRIORITY = 2

private const val IN_CLASS_TIMING_FULL = 0
private const val IN_CLASS_TIMING_BEFORE_END = 1

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hour = (minuteOfDay / 60).coerceIn(0, 23)
    val minute = (minuteOfDay % 60).coerceIn(0, 59)
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

/** 折叠展开的子项，与 Nexio 的 CardExpand 同义。 */
@Composable
private fun CardExpand(
    visible: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(content = content)
    }
}

@Composable
private fun PermissionStatus(
    granted: Boolean,
    grantedText: String = "已授权",
    deniedText: String = "未授权",
) {
    Text(
        text = if (granted) grantedText else deniedText,
        style = MiuixTheme.textStyles.body2,
        color =
            if (granted) {
                Color(0xFF4CAF50)
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantActions
            },
        textAlign = TextAlign.End,
    )
}

private data class LabelPreviewItem(val raw: String, val shown: String, val auto: String)

/**
 * 精简预览里的一行：左边课表原文，右边通知里会显示的文本，两边等宽。
 *
 * 有变化的右侧染主题色，扫一眼就知道哪几条动了。右侧那串**点一下就能改**：
 * 自动规则再细也有认不出的楼名，让本人直接改比堆提示词划算。改完的值按原文
 * 整串存进 [ClassroomLabel.KEY_OVERRIDES]，优先级压过正则与 AI。
 */
@Composable
private fun ClassroomPreviewRow(
    raw: String,
    shown: String,
    editing: Boolean,
    editText: String,
    onEditTextChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changed = shown != raw
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = raw,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "→",
            style = MiuixTheme.textStyles.body2,
            color =
                if (changed) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
        )
        if (editing) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            TextField(
                value = editText,
                onValueChange = onEditTextChange,
                singleLine = true,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
        } else {
            Text(
                text = shown,
                style = MiuixTheme.textStyles.body2,
                color =
                    if (changed) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                modifier = Modifier.weight(1f).clickable { onStartEdit() },
            )
        }
    }
}

/**
 * 精简预览弹窗，与 Nexio 的 OverlayDialog 同构：
 * 顶部方法名 + 改写条数，中间可滚动对照表，底部「恢复自动 / 完成」。
 */
@Composable
private fun ClassroomLabelPreviewDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    rule: ClassroomLabelRule,
    preview: ClassroomLabelPreview,
    onOverridesChange: (String) -> Unit,
) {
    var editingRaw by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var editAuto by remember { mutableStateOf("") }

    fun commitEdit() {
        val raw = editingRaw ?: return
        editingRaw = null
        val typed = editText.trim()
        // 改回自动结果 = 取消这一条手动改写
        val value = typed.takeIf { it.isNotEmpty() && it != editAuto }
        val text = ClassroomLabel.putOverride(rule.overrides, raw, value)
        if (text != rule.overrides) onOverridesChange(text)
    }

    // 「没有手动改写时的结果」：编辑框里改回它就等于取消这一条手动改写
    val autoRule = rule.copy(overrides = "")
    // 课程名走 applyCourse，地点走 apply —— 两者共用同一张对照表，只是正则方式的表不同
    val items =
        preview.courseNames.map {
            LabelPreviewItem(it, ClassroomLabel.applyCourse(it, rule), ClassroomLabel.applyCourse(it, autoRule))
        } +
            preview.classrooms.map {
                LabelPreviewItem(it, ClassroomLabel.apply(it, rule), ClassroomLabel.apply(it, autoRule))
            }
    val changedCount = items.count { it.raw != it.shown }
    val overriddenCount = items.count { rule.overrideMap.containsKey(it.raw) }

    OverlayDialog(
        show = show,
        liquidGlassBackdrop = LocalLiquidGlassBackdrop.current,
        title = "精简预览",
        summary = "点击右侧文字可手动修改",
        onDismissRequest = {
            // 顺手提交正在改的那一行：点遮罩/返回键不该悄悄丢掉刚打的字
            commitEdit()
            onDismissRequest()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =
                            if (rule.method == ClassroomLabel.METHOD_AI) {
                                "AI 精简"
                            } else {
                                "正则精简"
                            },
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "当前方式",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Text(
                    text = if (items.isEmpty()) "课表没有内容" else "$changedCount / ${items.size}",
                    fontSize = 19.sp,
                    color = MiuixTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "个会被改写",
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            // 整块一个圆角矩形，里面滚动
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                insideMargin = PaddingValues(0.dp),
            ) {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "课表里还没有课程或地点",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    ) {
                        itemsIndexed(items) { index, item ->
                            ClassroomPreviewRow(
                                raw = item.raw,
                                shown = item.shown,
                                editing = editingRaw == item.raw,
                                editText = editText,
                                onEditTextChange = { editText = it },
                                onStartEdit = {
                                    // 换一行改时先把上一行提交掉，不然那几笔白打了
                                    if (editingRaw != item.raw) commitEdit()
                                    editingRaw = item.raw
                                    editAuto = item.auto
                                    editText = item.shown
                                },
                                // 首尾各再让 6，凑成上下各 12，别贴着圆角
                                modifier =
                                    Modifier.padding(
                                        top = if (index == 0) 6.dp else 0.dp,
                                        bottom = if (index == items.lastIndex) 6.dp else 0.dp,
                                    ),
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 只改过才有「恢复自动」可点，没改过就别摆一个按不动的按钮
                if (overriddenCount > 0) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            editingRaw = null
                            onOverridesChange("")
                        },
                    ) {
                        Text(text = "恢复自动（$overriddenCount）")
                    }
                }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = {
                        commitEdit()
                        onDismissRequest()
                    },
                ) {
                    Text(text = "完成")
                }
            }
        }
    }
}

/**
 * 提醒设置页，结构与 Nexio 的 CourseReminderScreen 一致：
 * 总开关 → 提醒详情（下节课/次日/课中/勿扰/缩略态精简）→ 考试提醒 → 权限设置。
 */
@Composable
fun ReminderSettingsScreen(
    vm: SettingsViewModel,
    campus: Campus,
    termStartMs: Long,
    totalWeeks: Int,
    ensureCourseReminderPermission: (onGranted: () -> Unit) -> Unit = { it() },
    onRequestExactAlarmPermission: () -> Unit = {},
    onRequestDndPolicyPermission: () -> Unit = {},
    showMessage: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    // 与应用内其它下拉一致：菜单容器透明，由液态玻璃背板承载
    val glassDropdownColors =
        DropdownDefaults.dropdownColors(
            containerColor = Color.Transparent,
            selectedContainerColor = Color.Transparent,
        )
    val glassBackdrop = LocalSheetContentBackdrop.current ?: LocalLiquidGlassBackdrop.current
    val settingsUiState by vm.uiState.collectAsStateWithLifecycle()
    val exactAlarmState by vm.exactAlarmState.collectAsStateWithLifecycle()
    val leadMinutes by vm.reminderLeadMinutes.collectAsStateWithLifecycle()
    val nextDayEnabled by vm.nextDayReminderEnabled.collectAsStateWithLifecycle()
    val nextDayMinuteOfDay by vm.nextDayReminderMinuteOfDay.collectAsStateWithLifecycle()
    val inClassEnabled by vm.inClassReminderEnabled.collectAsStateWithLifecycle()
    val inClassTimingMode by vm.inClassTimingMode.collectAsStateWithLifecycle()
    val inClassLeadMinutes by vm.inClassLeadMinutes.collectAsStateWithLifecycle()
    val classDndEnabled by vm.classDndEnabled.collectAsStateWithLifecycle()
    val classDndMode by vm.classDndMode.collectAsStateWithLifecycle()
    val inExamEnabled by vm.inExamReminderEnabled.collectAsStateWithLifecycle()
    val inExamTimingMode by vm.inExamTimingMode.collectAsStateWithLifecycle()
    val inExamLeadMinutes by vm.inExamLeadMinutes.collectAsStateWithLifecycle()
    val examDndEnabled by vm.examDndEnabled.collectAsStateWithLifecycle()
    val examDndMode by vm.examDndMode.collectAsStateWithLifecycle()
    val islandEnabled by vm.islandEnabled.collectAsStateWithLifecycle()
    val islandLeftMode by vm.islandLeftMode.collectAsStateWithLifecycle()
    val islandRightMode by vm.islandRightMode.collectAsStateWithLifecycle()
    val islandAodMode by vm.islandAodMode.collectAsStateWithLifecycle()
    val permissionState by vm.reminderPermissionState.collectAsStateWithLifecycle()
    val labelScope by vm.classroomLabelScope.collectAsStateWithLifecycle()
    val labelMethod by vm.classroomLabelMethod.collectAsStateWithLifecycle()
    val labelAbbreviations by vm.classroomLabelAbbreviations.collectAsStateWithLifecycle()
    val labelOverrides by vm.classroomLabelOverrides.collectAsStateWithLifecycle()
    val labelPreview by vm.classroomLabelPreview.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) { vm.refreshReminderPermissionState() }
    LaunchedEffect(Unit) {
        vm.userMessages.collect { showMessage(it) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) vm.refreshReminderPermissionState()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val preClassEnabled = settingsUiState.reminderEnabled
    val masterEnabled = preClassEnabled || nextDayEnabled

    var showLeadPicker by remember { mutableStateOf(false) }
    var pickerMinutes by remember { mutableIntStateOf(leadMinutes) }

    var showNextDayPicker by remember { mutableStateOf(false) }
    var pickerHour by remember { mutableIntStateOf(nextDayMinuteOfDay / 60) }
    var pickerMinuteIndex by remember { mutableIntStateOf((nextDayMinuteOfDay % 60) / 5) }

    var showInClassTimingPicker by remember { mutableStateOf(false) }
    var pickerInClassMode by remember { mutableIntStateOf(inClassTimingMode) }
    var pickerInClassLead by remember { mutableIntStateOf(inClassLeadMinutes) }

    var showInExamTimingPicker by remember { mutableStateOf(false) }
    var pickerInExamMode by remember { mutableIntStateOf(inExamTimingMode) }
    var pickerInExamLead by remember { mutableIntStateOf(inExamLeadMinutes) }

    val classroomLabelEnabled = labelScope != ClassroomLabel.SCOPE_OFF
    val labelRule =
        remember(labelScope, labelMethod, labelAbbreviations, labelOverrides) {
            ClassroomLabelRule(
                enabled = classroomLabelEnabled,
                method = labelMethod,
                abbreviations = labelAbbreviations,
                scope = labelScope,
                overrides = labelOverrides,
            )
        }
    var showLabelPreview by remember { mutableStateOf(false) }
    var showAbbrevEditor by remember { mutableStateOf(false) }
    var abbrevDraft by remember { mutableStateOf("") }

    fun rescheduleCourse() {
        vm.scheduleCourseReminder(campus = campus, termStartMs = termStartMs, totalWeeks = totalWeeks)
    }

    // 考试提醒沿用课程提醒总开关：开课时提醒就顺带排上最近的考试
    fun scheduleExam() {
        vm.scheduleExamReminder(selectedTerm = settingsUiState.selectedTerm)
    }

    fun setMasterEnabled(enabled: Boolean) {
        if (enabled) {
            ensureCourseReminderPermission {
                onRequestExactAlarmPermission()
                vm.onReminderEnabledChanged(true)
                vm.onNextDayReminderEnabledChanged(true)
                rescheduleCourse()
                scheduleExam()
            }
        } else {
            vm.onReminderEnabledChanged(false)
            vm.onNextDayReminderEnabledChanged(false)
            vm.onInClassReminderEnabledChanged(false)
            vm.onClassDndEnabledChanged(false)
            vm.onInExamReminderEnabledChanged(false)
            vm.onExamDndEnabledChanged(false)
        }
    }

    AppPageScaffold(
        title = "提醒设置",
        onBack = onBack,
        popupHost = {
            if (showLeadPicker) {
                val items =
                    remember(leadMinutes) {
                        ReminderTiming.LEAD_MINUTES_RANGE.map { "$it 分钟" }.toPersistentList()
                    }
                WheelPickerBottomSheet(
                    show = showLeadPicker,
                    title = "提前提醒时间",
                    onDismissRequest = { showLeadPicker = false },
                    onConfirm = {
                        vm.onReminderLeadMinutesChanged(pickerMinutes)
                        showLeadPicker = false
                        if (masterEnabled) rescheduleCourse()
                    },
                ) {
                    WheelPickerRow(
                        columns =
                            listOf(
                                WheelPickerColumnState(
                                    key = "lead_minutes",
                                    items = items,
                                    selectedIndex = (pickerMinutes - 1).coerceIn(0, items.lastIndex),
                                    onSelectedIndexChange = { pickerMinutes = it + 1 },
                                )
                            )
                    )
                }
            }
            if (showNextDayPicker) {
                val hourItems =
                    remember { (0..23).map { "${it.toString().padStart(2, '0')} 时" }.toPersistentList() }
                val minuteItems =
                    remember { (0..11).map { "${(it * 5).toString().padStart(2, '0')} 分" }.toPersistentList() }
                WheelPickerBottomSheet(
                    show = showNextDayPicker,
                    title = "提醒时间",
                    onDismissRequest = { showNextDayPicker = false },
                    onConfirm = {
                        vm.onNextDayReminderMinuteChanged(pickerHour * 60 + pickerMinuteIndex * 5)
                        showNextDayPicker = false
                    },
                ) {
                    WheelPickerRow(
                        columns =
                            listOf(
                                WheelPickerColumnState(
                                    key = "next_day_hour",
                                    items = hourItems,
                                    selectedIndex = pickerHour.coerceIn(0, hourItems.lastIndex),
                                    onSelectedIndexChange = { pickerHour = it },
                                ),
                                WheelPickerColumnState(
                                    key = "next_day_minute",
                                    items = minuteItems,
                                    selectedIndex = pickerMinuteIndex.coerceIn(0, minuteItems.lastIndex),
                                    onSelectedIndexChange = { pickerMinuteIndex = it },
                                ),
                            )
                    )
                }
            }
            if (showInClassTimingPicker) {
                val modeItems = remember { listOf("全程", "距下课").toPersistentList() }
                val minuteItems =
                    remember { (0..60).map { "$it 分钟" }.toPersistentList() }
                WheelPickerBottomSheet(
                    show = showInClassTimingPicker,
                    title = "提醒时机",
                    onDismissRequest = { showInClassTimingPicker = false },
                    onConfirm = {
                        vm.onInClassTimingModeChanged(pickerInClassMode)
                        vm.onInClassLeadMinutesChanged(pickerInClassLead)
                        showInClassTimingPicker = false
                        if (inClassEnabled && masterEnabled) rescheduleCourse()
                    },
                ) {
                    WheelPickerRow(
                        columns =
                            buildList {
                                add(
                                    WheelPickerColumnState(
                                        key = "in_class_mode",
                                        items = modeItems,
                                        selectedIndex = pickerInClassMode.coerceIn(0, modeItems.lastIndex),
                                        onSelectedIndexChange = { pickerInClassMode = it },
                                    )
                                )
                                if (pickerInClassMode == IN_CLASS_TIMING_BEFORE_END) {
                                    add(
                                        WheelPickerColumnState(
                                            key = "in_class_lead",
                                            items = minuteItems,
                                            selectedIndex = pickerInClassLead.coerceIn(0, minuteItems.lastIndex),
                                            onSelectedIndexChange = { pickerInClassLead = it },
                                        )
                                    )
                                }
                            }
                    )
                }
            }
            if (showInExamTimingPicker) {
                val modeItems = remember { listOf("全程", "距结束").toPersistentList() }
                val minuteItems =
                    remember { (0..60).map { "$it 分钟" }.toPersistentList() }
                WheelPickerBottomSheet(
                    show = showInExamTimingPicker,
                    title = "提醒时机",
                    onDismissRequest = { showInExamTimingPicker = false },
                    onConfirm = {
                        vm.onInExamTimingModeChanged(pickerInExamMode)
                        vm.onInExamLeadMinutesChanged(pickerInExamLead)
                        showInExamTimingPicker = false
                    },
                ) {
                    WheelPickerRow(
                        columns =
                            buildList {
                                add(
                                    WheelPickerColumnState(
                                        key = "in_exam_mode",
                                        items = modeItems,
                                        selectedIndex = pickerInExamMode.coerceIn(0, modeItems.lastIndex),
                                        onSelectedIndexChange = { pickerInExamMode = it },
                                    )
                                )
                                if (pickerInExamMode == IN_CLASS_TIMING_BEFORE_END) {
                                    add(
                                        WheelPickerColumnState(
                                            key = "in_exam_lead",
                                            items = minuteItems,
                                            selectedIndex = pickerInExamLead.coerceIn(0, minuteItems.lastIndex),
                                            onSelectedIndexChange = { pickerInExamLead = it },
                                        )
                                    )
                                }
                            }
                    )
                }
            }
            if (showLabelPreview) {
                ClassroomLabelPreviewDialog(
                    show = showLabelPreview,
                    onDismissRequest = { showLabelPreview = false },
                    rule = labelRule,
                    preview = labelPreview,
                    onOverridesChange = { vm.onClassroomLabelOverridesChanged(it) },
                )
            }
            if (showAbbrevEditor) {
                OverlayDialog(
                    show = showAbbrevEditor,
                    liquidGlassBackdrop = LocalLiquidGlassBackdrop.current,
                    title = "缩写对照表",
                    summary = "每行一条「原名=缩写」，例如：第五教学楼=五教",
                    onDismissRequest = { showAbbrevEditor = false },
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = abbrevDraft,
                            onValueChange = { abbrevDraft = it },
                            label = "原名=缩写",
                            minLines = 6,
                            maxLines = 12,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { showAbbrevEditor = false },
                            ) {
                                Text(text = "取消")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                onClick = {
                                    vm.onClassroomLabelAbbreviationsChanged(abbrevDraft)
                                    showAbbrevEditor = false
                                },
                            ) {
                                Text(text = "完成")
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // 测试按钮照 Nexio 的 CourseReminderRoute：岛可用且开启时测岛，否则测实况
            if (permissionState.canSendTestNotification) {
                val islandTest = islandEnabled && permissionState.islandTierSupported
                Button(
                    onClick = { vm.sendTestReminderNotification(island = islandTest) },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    cornerRadius = 20.dp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 25.dp, start = 10.dp, end = 10.dp),
                ) {
                    Text(
                        text = if (islandTest) "测试小米超级岛" else "测试实时活动",
                        color = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { scrollBehavior, innerPadding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .collapsibleTopInset(scrollBehavior)
                    .pageScrollModifiers(scrollBehavior = scrollBehavior),
            contentPadding =
                appPageContentPadding(
                    innerPadding = innerPadding,
                    outerPadding = appScaffoldPadding,
                    extraTop = 12.dp,
                    extraStart = 12.dp,
                    extraEnd = 12.dp,
                ),
        ) {
            // 开启提醒
            item { SmallTitle(text = "开启提醒") }
            item {
                AppCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    SwitchPreference(
                        title = "开启提醒",
                        summary = "下节课、次日课程与上课勿扰的总开关",
                        checked = masterEnabled,
                        onCheckedChange = { setMasterEnabled(it) },
                    )
                }
            }

            // 提醒详情
            item { SmallTitle(text = "提醒详情") }
            item {
                AppCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    SwitchPreference(
                        title = "下节课提醒",
                        summary = "提供下节上课时间、地点等信息，和上课勿扰等操作建议",
                        checked = preClassEnabled,
                        enabled = masterEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                ensureCourseReminderPermission {
                                    vm.onReminderEnabledChanged(true)
                                    rescheduleCourse()
                                    scheduleExam()
                                }
                            } else {
                                vm.onReminderEnabledChanged(false)
                            }
                        },
                    )
                    CardExpand(visible = preClassEnabled) {
                        ArrowPreference(
                            title = "提前提醒时间",
                            endActions = {
                                Text(
                                    text = "$leadMinutes 分钟",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    textAlign = TextAlign.End,
                                )
                            },
                            onClick = {
                                pickerMinutes = leadMinutes
                                showLeadPicker = true
                            },
                        )
                    }
                    SwitchPreference(
                        title = "次日课程提醒",
                        summary = "提供明天课程、首节时间、地点等信息，和定闹钟等操作建议",
                        checked = nextDayEnabled,
                        enabled = masterEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                ensureCourseReminderPermission {
                                    vm.onNextDayReminderEnabledChanged(true)
                                }
                            } else {
                                vm.onNextDayReminderEnabledChanged(false)
                            }
                        },
                    )
                    CardExpand(visible = nextDayEnabled) {
                        ArrowPreference(
                            title = "提醒时间",
                            endActions = {
                                Text(
                                    text = formatMinuteOfDay(nextDayMinuteOfDay),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    textAlign = TextAlign.End,
                                )
                            },
                            onClick = {
                                pickerHour = nextDayMinuteOfDay / 60
                                pickerMinuteIndex = (nextDayMinuteOfDay % 60) / 5
                                showNextDayPicker = true
                            },
                        )
                    }
                    SwitchPreference(
                        title = "课中提醒",
                        summary = "上课中展示距离下课时间与课程状态",
                        checked = inClassEnabled,
                        enabled = masterEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                ensureCourseReminderPermission {
                                    vm.onInClassReminderEnabledChanged(true)
                                    rescheduleCourse()
                                }
                            } else {
                                vm.onInClassReminderEnabledChanged(false)
                                rescheduleCourse()
                            }
                        },
                    )
                    CardExpand(visible = masterEnabled && inClassEnabled) {
                        ArrowPreference(
                            title = "提醒时机",
                            endActions = {
                                Text(
                                    text =
                                        if (inClassTimingMode == IN_CLASS_TIMING_FULL) {
                                            "全程"
                                        } else {
                                            "距下课 $inClassLeadMinutes 分钟"
                                        },
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    textAlign = TextAlign.End,
                                )
                            },
                            onClick = {
                                pickerInClassMode = inClassTimingMode
                                pickerInClassLead = inClassLeadMinutes
                                showInClassTimingPicker = true
                            },
                        )
                    }
                    SwitchPreference(
                        title = "自动开启勿扰",
                        summary = "上课时自动开启勿扰，下课后自动恢复",
                        checked = classDndEnabled,
                        enabled = masterEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                onRequestDndPolicyPermission()
                                vm.onClassDndEnabledChanged(true)
                            } else {
                                vm.onClassDndEnabledChanged(false)
                            }
                            rescheduleCourse()
                        },
                    )
                    val dndModeEntry =
                        remember(classDndMode) {
                            DropdownEntry(
                                items =
                                    listOf(
                                        DropdownItem(
                                            text = "完全勿扰 (DND)",
                                            summary = "屏蔽全部通知与铃声",
                                            selected = classDndMode == DND_MODE_DND,
                                            onClick = {
                                                vm.onClassDndModeChanged(DND_MODE_DND)
                                                if (classDndEnabled && masterEnabled) rescheduleCourse()
                                            },
                                        ),
                                        DropdownItem(
                                            text = "静音模式 (SILENT)",
                                            summary = "关闭铃声与振动",
                                            selected = classDndMode == DND_MODE_SILENT,
                                            onClick = {
                                                vm.onClassDndModeChanged(DND_MODE_SILENT)
                                                if (classDndEnabled && masterEnabled) rescheduleCourse()
                                            },
                                        ),
                                        DropdownItem(
                                            text = "勿扰模式 (PRIORITY)",
                                            summary = "只放行优先通知",
                                            selected = classDndMode == DND_MODE_PRIORITY,
                                            onClick = {
                                                vm.onClassDndModeChanged(DND_MODE_PRIORITY)
                                                if (classDndEnabled && masterEnabled) rescheduleCourse()
                                            },
                                        ),
                                    )
                            )
                        }
                    CardExpand(visible = masterEnabled) {
                        GlassDropdownMenu(
                            title = "勿扰模式档位",
                            entry = dndModeEntry,
                            enabled = masterEnabled,
                            dropdownColors = glassDropdownColors,
                            liquidGlassBackdrop = glassBackdrop,
                            collapseOnSelection = true,
                        )
                    }
                }
            }

            // 缩略态精简（与 Nexio 的 reminder_thumb_label 同位置：提醒详情卡片之后）
            if (masterEnabled) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        val scopeEntry =
                            DropdownEntry(
                                items =
                                    listOf(
                                        ClassroomLabel.SCOPE_OFF to "原始文本",
                                        ClassroomLabel.SCOPE_CLASSROOM to "精简地点",
                                        ClassroomLabel.SCOPE_COURSE to "精简课程",
                                        ClassroomLabel.SCOPE_BOTH to "精简地点与课程",
                                    ).map { (value, text) ->
                                        DropdownItem(
                                            text = text,
                                            selected = labelScope == value,
                                            onClick = { vm.onClassroomLabelScopeChanged(value) },
                                        )
                                    }
                            )
                        GlassDropdownMenu(
                            title = "缩略态精简",
                            summary = "超级岛缩略态的地点与课程名显示方式",
                            entry = scopeEntry,
                            dropdownColors = glassDropdownColors,
                            liquidGlassBackdrop = glassBackdrop,
                            collapseOnSelection = true,
                        )
                        CardExpand(visible = classroomLabelEnabled) {
                            val methodEntry =
                                DropdownEntry(
                                    items =
                                        listOf(
                                            DropdownItem(
                                                text = "正则精简",
                                                summary = "按预设规则对地点进行精简",
                                                selected = labelMethod == ClassroomLabel.METHOD_REGEX,
                                                onClick = {
                                                    vm.onClassroomLabelMethodChanged(ClassroomLabel.METHOD_REGEX)
                                                },
                                            ),
                                            DropdownItem(
                                                text = "AI 精简",
                                                summary = "按「原名=缩写」对照表精简",
                                                selected = labelMethod == ClassroomLabel.METHOD_AI,
                                                onClick = {
                                                    vm.onClassroomLabelMethodChanged(ClassroomLabel.METHOD_AI)
                                                },
                                            ),
                                        )
                                )
                            GlassDropdownMenu(
                                title = "精简方式",
                                entry = methodEntry,
                                dropdownColors = glassDropdownColors,
                                liquidGlassBackdrop = glassBackdrop,
                                collapseOnSelection = true,
                            )
                            if (labelMethod == ClassroomLabel.METHOD_AI) {
                                ArrowPreference(
                                    title = "缩写对照表",
                                    summary = "每行一条「原名=缩写」，留空时保持原文",
                                    endActions = {
                                        Text(
                                            text = "编辑",
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                            textAlign = TextAlign.End,
                                        )
                                    },
                                    onClick = {
                                        abbrevDraft = labelAbbreviations
                                        showAbbrevEditor = true
                                    },
                                )
                            }
                            ArrowPreference(
                                title = "查看精简预览",
                                summary = "预览精简后的课程地点",
                                onClick = {
                                    vm.loadClassroomLabelPreview()
                                    showLabelPreview = true
                                },
                            )
                        }
                    }
                }
            }

            // 考试提醒（考试沿用课程提醒总开关，这里只放考试专属的考试中与勿扰）
            if (masterEnabled) {
                item { SmallTitle(text = "考试提醒") }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        SwitchPreference(
                            title = "考试中提醒",
                            summary = "考试中展示距离结束时间与考试状态",
                            checked = inExamEnabled,
                            enabled = masterEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    ensureCourseReminderPermission {
                                        vm.onInExamReminderEnabledChanged(true)
                                    }
                                } else {
                                    vm.onInExamReminderEnabledChanged(false)
                                }
                            },
                        )
                        CardExpand(visible = masterEnabled && inExamEnabled) {
                            ArrowPreference(
                                title = "提醒时机",
                                endActions = {
                                    Text(
                                        text =
                                            if (inExamTimingMode == IN_CLASS_TIMING_FULL) {
                                                "全程"
                                            } else {
                                                "距结束 $inExamLeadMinutes 分钟"
                                            },
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        textAlign = TextAlign.End,
                                    )
                                },
                                onClick = {
                                    pickerInExamMode = inExamTimingMode
                                    pickerInExamLead = inExamLeadMinutes
                                    showInExamTimingPicker = true
                                },
                            )
                        }
                        SwitchPreference(
                            title = "考试自动勿扰",
                            summary = "开考时自动开启勿扰，考试结束后自动恢复",
                            checked = examDndEnabled,
                            enabled = masterEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    onRequestDndPolicyPermission()
                                    vm.onExamDndEnabledChanged(true)
                                } else {
                                    vm.onExamDndEnabledChanged(false)
                                }
                                scheduleExam()
                            },
                        )
                        val examDndModeEntry =
                            remember(examDndMode) {
                                DropdownEntry(
                                    items =
                                        listOf(
                                            DropdownItem(
                                                text = "完全勿扰 (DND)",
                                                summary = "屏蔽全部通知与铃声",
                                                selected = examDndMode == DND_MODE_DND,
                                                onClick = {
                                                    vm.onExamDndModeChanged(DND_MODE_DND)
                                                    if (examDndEnabled && masterEnabled) scheduleExam()
                                                },
                                            ),
                                            DropdownItem(
                                                text = "静音模式 (SILENT)",
                                                summary = "关闭铃声与振动",
                                                selected = examDndMode == DND_MODE_SILENT,
                                                onClick = {
                                                    vm.onExamDndModeChanged(DND_MODE_SILENT)
                                                    if (examDndEnabled && masterEnabled) scheduleExam()
                                                },
                                            ),
                                            DropdownItem(
                                                text = "勿扰模式 (PRIORITY)",
                                                summary = "只放行优先通知",
                                                selected = examDndMode == DND_MODE_PRIORITY,
                                                onClick = {
                                                    vm.onExamDndModeChanged(DND_MODE_PRIORITY)
                                                    if (examDndEnabled && masterEnabled) scheduleExam()
                                                },
                                            ),
                                        )
                                )
                            }
                        GlassDropdownMenu(
                            title = "考试勿扰档位",
                            entry = examDndModeEntry,
                            enabled = masterEnabled,
                            dropdownColors = glassDropdownColors,
                            liquidGlassBackdrop = glassBackdrop,
                            collapseOnSelection = true,
                        )
                    }
                }
            }

            // 超级岛（仅 HyperOS 设备显示，与 Nexio 的 showIslandCard 一致）
            if (permissionState.islandSupported) {
                item { SmallTitle(text = "超级岛") }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        SwitchPreference(
                            title = "小米超级岛",
                            summary =
                                if (islandEnabled) {
                                    "已开启，课程提醒将以超级岛样式显示"
                                } else {
                                    "关闭后使用实时活动通知"
                                },
                            checked = islandEnabled,
                            onCheckedChange = { vm.onIslandEnabledChanged(it) },
                        )
                        val slotOptions: (Int, (Int) -> Unit) -> DropdownEntry =
                            { selected, onSelect ->
                                DropdownEntry(
                                    items =
                                        listOf(
                                            DropdownItem(
                                                text = "课程名称",
                                                selected = selected == 0,
                                                onClick = { onSelect(0) },
                                            ),
                                            DropdownItem(
                                                text = "上课地点",
                                                selected = selected == 1,
                                                onClick = { onSelect(1) },
                                            ),
                                            DropdownItem(
                                                text = "倒计时",
                                                selected = selected == 2,
                                                onClick = { onSelect(2) },
                                            ),
                                        )
                                )
                            }
                        CardExpand(visible = islandEnabled) {
                            // Shizuku 状态：HyperOS 专属，照 Nexio 的 shizuku_state 行。
                            // 发岛前用它临时掐掉 XMSF 网络，否则焦点通知会被服务端接管成普通推送
                            if (permissionState.islandTierSupported) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(bottom = 8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Shizuku 状态",
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text =
                                                when {
                                                    !permissionState.shizukuRunning -> "未运行"
                                                    !permissionState.shizukuAuthorized -> "未授权"
                                                    else -> "就绪"
                                                },
                                            style = MiuixTheme.textStyles.body2,
                                            color =
                                                when {
                                                    !permissionState.shizukuRunning -> Color(0xFFFF6B6B)
                                                    !permissionState.shizukuAuthorized -> Color(0xFFFFB347)
                                                    else -> Color(0xFF4CAF50)
                                                },
                                            textAlign = TextAlign.End,
                                        )
                                    }
                                    if (!permissionState.shizukuRunning) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "请安装并启动 Shizuku 应用",
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        )
                                    }
                                    if (permissionState.shizukuRunning &&
                                        !permissionState.shizukuAuthorized
                                    ) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TextButton(
                                            text = "授权 Shizuku",
                                            onClick = { vm.requestShizukuPermission() },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                            GlassDropdownMenu(
                                title = "超级岛左侧",
                                entry = slotOptions(islandLeftMode) { vm.onIslandLeftModeChanged(it) },
                                dropdownColors = glassDropdownColors,
                                liquidGlassBackdrop = glassBackdrop,
                                collapseOnSelection = true,
                            )
                            GlassDropdownMenu(
                                title = "超级岛右侧",
                                entry = slotOptions(islandRightMode) { vm.onIslandRightModeChanged(it) },
                                dropdownColors = glassDropdownColors,
                                liquidGlassBackdrop = glassBackdrop,
                                collapseOnSelection = true,
                            )
                            GlassDropdownMenu(
                                title = "息屏显示",
                                summary = "全天候显示时展示的内容",
                                entry =
                                    DropdownEntry(
                                        items =
                                            listOf(
                                                DropdownItem(
                                                    text = "课程名称",
                                                    selected = islandAodMode == 0,
                                                    onClick = { vm.onIslandAodModeChanged(0) },
                                                ),
                                                DropdownItem(
                                                    text = "上课地点",
                                                    selected = islandAodMode == 1,
                                                    onClick = { vm.onIslandAodModeChanged(1) },
                                                ),
                                            )
                                    ),
                                dropdownColors = glassDropdownColors,
                                liquidGlassBackdrop = glassBackdrop,
                                collapseOnSelection = true,
                            )
                        }
                    }
                }
            }

            // 权限设置
            item { SmallTitle(text = "权限设置") }
            item {
                AppCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "通知权限",
                        summary = "用于接收课程提醒通知",
                        endActions = {
                            PermissionStatus(granted = permissionState.notificationGranted)
                        },
                        onClick = {
                            if (!permissionState.notificationGranted) {
                                ensureCourseReminderPermission {}
                            }
                        },
                    )
                    ArrowPreference(
                        title = "精确闹钟权限",
                        summary = "确保提醒准时触发，不受省电策略影响",
                        endActions = {
                            PermissionStatus(
                                granted = exactAlarmState == ExactAlarmPermissionState.GRANTED,
                            )
                        },
                        onClick = onRequestExactAlarmPermission,
                    )
                    ArrowPreference(
                        title = "电池优化",
                        summary = "关闭电池优化以确保提醒准时送达",
                        endActions = {
                            PermissionStatus(
                                granted = permissionState.ignoringBatteryOptimizations,
                                grantedText = "已关闭",
                                deniedText = "未关闭",
                            )
                        },
                        onClick = {
                            if (!permissionState.ignoringBatteryOptimizations) {
                                vm.openBatteryOptimizationSettings()
                            }
                        },
                    )
                    ArrowPreference(
                        title = "勿扰权限",
                        summary = "用于上课时开启勿扰或静音",
                        endActions = {
                            PermissionStatus(granted = permissionState.dndGranted)
                        },
                        onClick = {
                            if (!permissionState.dndGranted) {
                                onRequestDndPolicyPermission()
                            }
                        },
                    )
                    ArrowPreference(
                        title = "自启动权限",
                        summary = "在应用被系统清理后及时重启",
                        endActions = {
                            Text(
                                text = "前往检查",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                textAlign = TextAlign.End,
                            )
                        },
                        onClick = { vm.openAppDetailsSettings() },
                    )
                }
            }

        }
    }
}
