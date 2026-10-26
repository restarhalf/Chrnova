package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.usecase.TransCourseBatchUseCase
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.SectionRangePickerBottomSheet
import restarhalf.stellar.schedule.ui.components.WeekPalette
import restarhalf.stellar.schedule.ui.components.WeekdayPickerBottomSheet
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Change
import restarhalf.stellar.schedule.ui.icons.Check
import restarhalf.stellar.schedule.ui.chrome.AppPageTopBar
import restarhalf.stellar.schedule.ui.chrome.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.chrome.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.CourseEditViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CourseEditScreen(
    vm: CourseEditViewModel,
    onBack: () -> Unit,
    isEdit: Boolean,
    onEditChanged: (Boolean) -> Unit,
    courseId: Long? = null,
    initialDayOfWeek: Int = 1,
    initialStartSection: Int = 1,
    initialSelectedWeek: Int = 1,
    totalWeeks: Int = 20,
    showMessage: (String) -> Unit = {},
    transMode: Boolean = false,
) {
    val changeToSelect = remember { mutableStateOf(true) }
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val courseEditUiState by vm.uiState.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val editingCourse by
    remember(courseId) { vm.observeEditingCourse(courseId) }
        .collectAsStateWithLifecycle(initialValue = null)

    val courseNames = courseEditUiState.courseNames
    val inputCourseName = remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var courseNameError by remember { mutableStateOf(false) }
    val classRoomValue = remember { mutableStateOf("") }
    val teacherValue = remember { mutableStateOf("") }
    val showWeekdayPicker = remember { mutableStateOf(false) }
    val showSectionPicker = remember { mutableStateOf(false) }
    var dayOfWeek by remember { mutableIntStateOf(1) }
    var startSection by remember { mutableIntStateOf(1) }
    var endSection by remember { mutableIntStateOf(2) }
    var selectedWeeks by remember { mutableStateOf(emptySet<Int>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var savingTrans by remember { mutableStateOf(false) }
    var showTransConflict by remember { mutableStateOf(false) }
    var conflictWeeks by remember {
        mutableStateOf(emptyList<TransCourseBatchUseCase.WeekConflict>())
    }
    var pendingOverrides by remember { mutableStateOf(emptyList<Course>()) }
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(
        courseId,
        editingCourse,
        courseNames.size,
        courseNames.hashCode(),
        initialDayOfWeek,
        initialStartSection,
        initialSelectedWeek
    ) {
        if (transMode) {
            val formState = vm.buildTransFormState(editingCourse)
            onEditChanged(formState.isReady)
            classRoomValue.value = formState.classRoom
            dayOfWeek = formState.dayOfWeek
            startSection = formState.startSection
            endSection = formState.endSection
            selectedWeeks = formState.selectedWeeks
            return@LaunchedEffect
        }
        val formState = vm.buildEditingFormState(courseId, editingCourse, courseNames, initialDayOfWeek, initialStartSection, initialSelectedWeek)
        onEditChanged(formState.isEdit)
        selectedIndex = formState.selectedIndex
        classRoomValue.value = formState.classRoom
        teacherValue.value = formState.teacher
        dayOfWeek = formState.dayOfWeek
        startSection = formState.startSection
        endSection = formState.endSection
        selectedWeeks = formState.selectedWeeks
    }

    fun submitTrans(overrides: List<Course>) {
        val origin = editingCourse ?: return
        if (savingTrans) return
        savingTrans = true
        scope.launch {
            try {
                val saved = vm.saveTransBatch(origin, overrides)
                if (saved) {
                    showTransConflict = false
                    onBack()
                } else {
                    showMessage("保存失败，请重试")
                }
            } finally {
                savingTrans = false
            }
        }
    }

    LaunchedEffect(selectedIndex, inputCourseName.value, changeToSelect.value, courseNames.size) {
        if (courseId != null) return@LaunchedEffect
        val selectedName = if (changeToSelect.value) {
            courseNames.getOrNull(selectedIndex).orEmpty()
        } else {
            inputCourseName.value
        }
        val matchedTeacher = courseEditUiState.courses
            .firstOrNull { it.type == 0 && it.name == selectedName && it.teacher.isNotBlank() }
            ?.teacher
            .orEmpty()
        teacherValue.value = matchedTeacher
    }

    val weekdayText = remember(dayOfWeek) { vm.weekdayText(dayOfWeek) }
    val transMaxWeeks = remember(editingCourse) { vm.transMaxWeeks(editingCourse) }

    val disabledWeeks =
        remember(
            courseEditUiState.courses,
            dayOfWeek,
            startSection,
            endSection,
            courseId,
            editingCourse?.id,
            transMode
        ) {
            if (transMode) {
                emptySet()
            } else {
                vm.buildDisabledWeeks(
                    courses = courseEditUiState.courses,
                    dayOfWeek = dayOfWeek,
                    startSection = startSection,
                    endSection = endSection,
                    courseId = courseId,
                    editingCourseId = editingCourse?.id,
                )
            }
        }
    Scaffold(
        topBar = {
            AppPageTopBar(
                title = if (transMode) "整门课调课" else "编辑实验课",
                scrollBehavior = topAppBarScrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            if (transMode) {
                                val origin = editingCourse
                                if (origin == null) {
                                    showMessage("课程信息未加载")
                                    return@IconButton
                                }
                                if (selectedWeeks.isEmpty()) {
                                    showMessage("请至少选择一个调课周")
                                    return@IconButton
                                }
                                if (savingTrans) return@IconButton
                                savingTrans = true
                                scope.launch {
                                    try {
                                        val result =
                                            vm.buildTransBatch(
                                                originCourse = origin,
                                                weeks = selectedWeeks,
                                                classRoom = classRoomValue.value,
                                                dayOfWeek = dayOfWeek,
                                                startSection = startSection,
                                                endSection = endSection,
                                            )
                                        if (result == null) {
                                            showMessage("调课失败，请重试")
                                            return@launch
                                        }
                                        if (!result.hasChanges) {
                                            showMessage("时间和教室都没变，无需调课")
                                            return@launch
                                        }
                                        if (result.hasConflicts) {
                                            conflictWeeks = result.weekConflicts
                                            pendingOverrides = result.overrideCourses
                                            showTransConflict = true
                                            return@launch
                                        }
                                        val saved = vm.saveTransBatch(origin, result.overrideCourses)
                                        if (saved) {
                                            onBack()
                                        } else {
                                            showMessage("保存失败，请重试")
                                        }
                                    } finally {
                                        savingTrans = false
                                    }
                                }
                                return@IconButton
                            }
                            val selectedName = if(changeToSelect.value){courseNames.getOrNull(selectedIndex).orEmpty()}else{inputCourseName.value}
                            if (selectedName.isBlank()) {
                                courseNameError = true
                                showMessage("请输入课程名")
                                return@IconButton
                            }
                            val toSave =
                                vm.buildLabCourseToSave(
                                    selectedName = selectedName,
                                    selectedWeeks = selectedWeeks,
                                    classRoom = classRoomValue.value,
                                    teacher = teacherValue.value,
                                    dayOfWeek = dayOfWeek,
                                    startSection = startSection,
                                    endSection = endSection,
                                    existing = editingCourse,
                                    courses = courseEditUiState.courses
                                )
                            if (toSave == null) {
                                showMessage("请至少选择一个上课周")
                                return@IconButton
                            }
                            vm.saveLabCourse(toSave) { saved ->
                                if (saved) {
                                    onBack()
                                } else {
                                    showMessage("保存失败，请重试")
                                }
                            }
                        },
                        enabled = !isSaving && !savingTrans
                    ) {
                        Icon(imageVector = Check, contentDescription = "确定")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回"
                        )
                    }
                },
            )
        },
        popupHost = {
            if (showWeekdayPicker.value) {
                WeekdayPickerBottomSheet(
                    show = showWeekdayPicker.value,
                    title = "上课星期",
                    initialDayOfWeek = dayOfWeek,
                    onDismissRequest = { showWeekdayPicker.value = false },
                    onConfirm = { newDayOfWeek ->
                        dayOfWeek = newDayOfWeek
                        showWeekdayPicker.value = false
                    },
                )
            }

            if (showSectionPicker.value) {
                SectionRangePickerBottomSheet(
                    show = showSectionPicker.value,
                    title = "上课时间",
                    sectionRange = 1..12,
                    initialStartSection = startSection,
                    initialEndSection = endSection,
                    onDismissRequest = { showSectionPicker.value = false },
                    onConfirm = { newStartSection, newEndSection ->
                        startSection = newStartSection
                        endSection = newEndSection
                        showSectionPicker.value = false
                    })
            }

            if (showTransConflict) {
                WindowDialog(
                    show = showTransConflict,
                    title = "调课冲突",
                    summary = "以下周次的目标时间段已有课程",
                    onDismissRequest = {
                        showTransConflict = false
                        pendingOverrides = emptyList()
                        conflictWeeks = emptyList()
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conflictWeeks) { weekConflict ->
                            Column {
                                Text(
                                    text = "第${weekConflict.week}周",
                                    style = MiuixTheme.textStyles.body1,
                                    fontWeight = FontWeight.Bold
                                )
                                weekConflict.courses.forEach { c ->
                                    Text(
                                        text = "${c.name} @${c.location}   |   第${c.startSection}-${c.startSection + c.sectionCount - 1}节",
                                        style = MiuixTheme.textStyles.footnote1
                                    )
                                }
                            }
                        }
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showTransConflict = false
                                        pendingOverrides = emptyList()
                                        conflictWeeks = emptyList()
                                    }
                                ) {
                                    Text(text = "取消")
                                }
                                Button(
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    onClick = {
                                        val overrides = pendingOverrides
                                        pendingOverrides = emptyList()
                                        conflictWeeks = emptyList()
                                        submitTrans(overrides)
                                    }
                                ) {
                                    Text(text = "强制保存", color = colors.onPrimary)
                                }
                            }
                        }
                    }
                }
            }

            if (showDeleteConfirm) {
                WindowDialog(
                    show = showDeleteConfirm,
                    title = "确认删除",
                    summary = "删除后不可恢复，确定要删除这门课程吗？",
                    onDismissRequest = { showDeleteConfirm = false }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showDeleteConfirm = false }
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = {
                                val c = editingCourse ?: return@Button
                                vm.deleteCourse(c) { deleted ->
                                    if (deleted) {
                                        onBack()
                                    } else {
                                        showMessage("删除失败，请重试")
                                    }
                                }
                                showDeleteConfirm = false
                            }
                        ) {
                            Text(text = "确定", color = colors.onPrimary)
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isEdit && !transMode) {
                Button(
                    onClick = {
                        showDeleteConfirm = true
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    cornerRadius = 20.dp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 25.dp, start = 10.dp, end = 10.dp)
                ) {
                    Text(text = "删除课程", color = colors.onPrimary)
                }
            }
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding =
                appPageContentPadding(
                    innerPadding = paddingValues,
                    outerPadding = appScaffoldPadding,
                    extraTop = 12.dp,
                    extraStart = 12.dp,
                    extraEnd = 12.dp,
                ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            overscrollEffect = null
        ) {
            if (transMode) {
                item {
                    AppCard {
                        BasicComponent(
                            title = editingCourse?.name ?: "课程信息加载中",
                            summary = editingCourse?.let { c ->
                                "${c.teacher}   |   原${vm.weekdayText(c.dayOfWeek)} " +
                                    "第${c.startSection}-${c.startSection + c.sectionCount - 1}节   |   ${c.location}"
                            } ?: "",
                        )
                    }
                }
            } else {
                item {
                    AppCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (changeToSelect.value) {
                                OverlayDropdownPreference(
                                    title = "课程名",
                                    items = courseNames,
                                    selectedIndex = selectedIndex.coerceIn(
                                        0,
                                        (courseNames.size - 1).coerceAtLeast(0)
                                    ),
                                    onSelectedIndexChange = { index: Int -> selectedIndex = index },
                                )
                            } else {
                                TextField(
                                    label = "课程名",
                                    value = inputCourseName.value,
                                    onValueChange = {
                                        inputCourseName.value = it
                                        courseNameError = false
                                    },
                                    colors = if (courseNameError) TextFieldDefaults.textFieldColors(
                                        borderColor = colors.error
                                    ) else TextFieldDefaults.textFieldColors()
                                )
                            }
                            IconButton(
                                onClick = { changeToSelect.value = !changeToSelect.value }
                            ) {
                                Icon(
                                    imageVector = Change,
                                    contentDescription = "切换"
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    TextField(
                        label = "教室",
                        value = classRoomValue.value,
                        onValueChange = { classRoomValue.value = it })
                }
            }
            if (!transMode) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    AppCard {
                        TextField(
                            label = "教师",
                            value = teacherValue.value,
                            onValueChange = { teacherValue.value = it })
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    ArrowPreference(
                        title = if (transMode) "调到星期" else "上课星期",
                        summary = weekdayText,
                        onClick = { showWeekdayPicker.value = true })
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    ArrowPreference(
                        title = if (transMode) "调到时间" else "上课时间",
                        summary = vm.sectionSummary(startSection, endSection),
                        onClick = { showSectionPicker.value = true })
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    BasicComponent {
                        Text(text = if (transMode) "调课周数" else "上课周数")
                        if (transMode) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "这门课共 ${transMaxWeeks} 周，最多选 ${transMaxWeeks} 周；取消的周可换成新周",
                                style = MiuixTheme.textStyles.footnote1,
                                color = colors.onSurfaceVariantSummary,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        WeekPalette(
                            weeks = totalWeeks,
                            selectedWeeks = selectedWeeks,
                            disabledWeeks = disabledWeeks,
                            onToggleWeek = { week ->
                                if (transMode) {
                                    val next = vm.toggleTransWeek(selectedWeeks, week, transMaxWeeks)
                                    if (next == selectedWeeks && !selectedWeeks.contains(week)) {
                                        showMessage("这门课只有 ${transMaxWeeks} 周，最多选 ${transMaxWeeks} 周")
                                    }
                                    selectedWeeks = next
                                } else {
                                    selectedWeeks = vm.toggleWeek(selectedWeeks, week)
                                }
                            },
                        )
                    }
                }

            }
        }
    }
}
