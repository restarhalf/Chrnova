package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.SectionRangePickerBottomSheet
import restarhalf.stellar.schedule.ui.components.WeekPalette
import restarhalf.stellar.schedule.ui.components.WeekdayPickerBottomSheet
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Change
import restarhalf.stellar.schedule.ui.icons.Check
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
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
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 课程编辑页面屏幕
 * 
 * 用于添加或编辑实验课，包括：
 * - 选择课程名称
 * - 设置教室
 * - 选择星期几
 * - 选择节次范围
 * - 选择上课周次
 * - 保存/删除操作
 * 
 * @param onBack 返回回调
 * @param isEdit 是否为编辑模式
 * @param courseId 课程ID（编辑时使用）
 * @param initialDayOfWeek 预选星期几（1-7），用于新建时预填
 * @param initialStartSection 预选开始节次（1-12），用于新建时预填
 * @param initialSelectedWeek 预选周次（1-n），用于新建时预填
 */
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
) {
    val changeToSelect = remember { mutableStateOf(true) }
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val courseEditUiState by vm.uiState.collectAsState()

    val editingCourse by
    remember(courseId) { vm.observeEditingCourse(courseId) }
        .collectAsState(initial = null)

    val courseNames = courseEditUiState.courseNames
    val inputCourseName = remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var courseNameError by remember { mutableStateOf(false) }
    val classRoomValue = remember { mutableStateOf("") }
    val showWeekdayPicker = remember { mutableStateOf(false) }
    val showSectionPicker = remember { mutableStateOf(false) }
    var dayOfWeek by remember { mutableIntStateOf(1) }
    var startSection by remember { mutableIntStateOf(1) }
    var endSection by remember { mutableIntStateOf(2) }
    var selectedWeeks by remember { mutableStateOf(emptySet<Int>()) }

    LaunchedEffect(courseId, editingCourse, courseNames, initialDayOfWeek, initialStartSection, initialSelectedWeek) {
        val formState = vm.buildEditingFormState(courseId, editingCourse, courseNames, initialDayOfWeek, initialStartSection, initialSelectedWeek)
        onEditChanged(formState.isEdit)
        selectedIndex = formState.selectedIndex
        classRoomValue.value = formState.classRoom
        dayOfWeek = formState.dayOfWeek
        startSection = formState.startSection
        endSection = formState.endSection
        selectedWeeks = formState.selectedWeeks
    }

    val weekdayText = remember(dayOfWeek) { vm.weekdayText(dayOfWeek) }

    val disabledWeeks =
        remember(courseEditUiState.courses, dayOfWeek, startSection, endSection, courseId, editingCourse?.id) {
            vm.buildDisabledWeeks(
                courses = courseEditUiState.courses,
                dayOfWeek = dayOfWeek,
                startSection = startSection,
                endSection = endSection,
                courseId = courseId,
                editingCourseId = editingCourse?.id,
            )
        }
    Scaffold(
        topBar = {
            AppPageTopBar(
                title = "编辑实验课",
                scrollBehavior = topAppBarScrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            val selectedName = if(changeToSelect.value){courseNames.getOrNull(selectedIndex).orEmpty()}else{inputCourseName.value}
                            if (selectedName.isBlank()) {
                                courseNameError = true
                                AppLogger.log("EDIT", level = AppLogger.Level.ERROR, message = "课名为空，保存失败")
                                return@IconButton
                            }
                            val toSave =
                                vm.buildLabCourseToSave(
                                    selectedName = selectedName,
                                    selectedWeeks = selectedWeeks,
                                    classRoom = classRoomValue.value,
                                    dayOfWeek = dayOfWeek,
                                    startSection = startSection,
                                    endSection = endSection,
                                    existing = editingCourse,
                                    courses = courseEditUiState.courses
                                )
                            if (toSave != null) {
                                vm.saveLabCourse(toSave, onSaved = onBack)
                            }
                        }) {
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
        },
        bottomBar = {
            if (isEdit) {
                Button(
                    onClick = {
                        val c = editingCourse ?: return@Button
                        vm.deleteCourse(c, onDeleted = onBack)
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    cornerRadius = 20.dp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 25.dp, start = 10.dp, end = 10.dp)
                ) {
                    Text(text = "删除课程", color = MiuixTheme.colorScheme.onPrimary)
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
                    extraStart = 16.dp,
                    extraEnd = 16.dp,
                ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            overscrollEffect = null
        ) {
            item {

                    AppCard {
                        Column (horizontalAlignment = Alignment.CenterHorizontally){
                        if(changeToSelect.value){
                            OverlayDropdownPreference(
                                title = "课程名",
                                items = courseNames,
                                selectedIndex = selectedIndex.coerceIn(
                                    0,
                                    (courseNames.size - 1).coerceAtLeast(0)
                                ),
                                onSelectedIndexChange = { index: Int -> selectedIndex = index },
                            )
                        }else{
                            TextField(
                                label = "课程名",
                                value = inputCourseName.value,
                                onValueChange = {
                                    inputCourseName.value=it
                                    courseNameError=false
                                },
                                colors = if(courseNameError) TextFieldDefaults.textFieldColors(borderColor = MiuixTheme.colorScheme.error) else TextFieldDefaults.textFieldColors()
                            )
                        }
                            IconButton(
                                onClick = {changeToSelect.value=!changeToSelect.value}
                            ){
                                Icon(
                                    imageVector = Change,
                                    contentDescription = "切换"
                                )
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
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    ArrowPreference(
                        title = "上课星期",
                        summary = weekdayText,
                        onClick = { showWeekdayPicker.value = true })
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    ArrowPreference(
                        title = "上课时间",
                        summary = vm.sectionSummary(startSection, endSection),
                        onClick = { showSectionPicker.value = true })
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    BasicComponent {
                        Text(text = "上课周数")
                        Spacer(Modifier.height(10.dp))
                        WeekPalette(
                            selectedWeeks = selectedWeeks,
                            disabledWeeks = disabledWeeks,
                            onToggleWeek = { week ->
                                selectedWeeks = vm.toggleWeek(selectedWeeks, week)
                            },
                        )
                    }
                }

            }
        }
    }
}
