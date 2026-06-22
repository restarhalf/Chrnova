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
import androidx.compose.runtime.MutableState
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.DatePickerBottomSheet
import restarhalf.stellar.schedule.ui.components.TimeRangePickerBottomSheet
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Change
import restarhalf.stellar.schedule.ui.icons.Check
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.ExamEditViewModel
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private fun padTwo(n: Int): String = if (n < 10) "0$n" else "$n"

@OptIn(ExperimentalTime::class)
@Composable
fun ExamEditScreen(
    onBack: () -> Unit,
    isEdit: MutableState<Boolean>,
    examinationId: Long? = null,
) {
    val changeToSelect = remember { mutableStateOf(true) }
    val vm: ExamEditViewModel = koinViewModel()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val examEditUiState by vm.uiState.collectAsState()

    val editingExamination by
    remember(examinationId) { vm.observeEditingExamination(examinationId) }
        .collectAsState(initial = null)

    val courseNames = examEditUiState.courseNames
    val courses = examEditUiState.courses
    val inputCourseName = remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var courseNameError by remember { mutableStateOf(false) }
    val datePartValue = remember { mutableStateOf("") }
    val timePartValue = remember { mutableStateOf("") }
    val examinationPlaceValue = remember { mutableStateOf("") }
    val zwhValue = remember { mutableStateOf("") }
    val ksbzValue = remember { mutableStateOf("") }

    val showDatePicker = remember { mutableStateOf(false) }
    val showTimePicker = remember { mutableStateOf(false) }

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var pickerDate by remember { mutableStateOf(now) }
    var pickerStartHour by remember { mutableIntStateOf(8) }
    var pickerStartMinute by remember { mutableIntStateOf(0) }
    var pickerEndHour by remember { mutableIntStateOf(10) }
    var pickerEndMinute by remember { mutableIntStateOf(0) }

    LaunchedEffect(examinationId, editingExamination, courseNames, courses) {
        val formState = vm.buildEditingFormState(examinationId, editingExamination, courseNames)
        isEdit.value = formState.isEdit
        selectedIndex = formState.selectedIndex
        inputCourseName.value = formState.courseName
        datePartValue.value = formState.datePart
        timePartValue.value = formState.timePart
        examinationPlaceValue.value = formState.examinationPlace
        zwhValue.value = formState.zwh
        ksbzValue.value = formState.ksbz

        if (formState.datePart.isNotBlank()) {
            try {
                pickerDate = LocalDate.parse(formState.datePart)
            } catch (_: Exception) {}
        }
        if (formState.timePart.isNotBlank()) {
            try {
                val parts = formState.timePart.split("-")
                if (parts.size == 2) {
                    val startParts = parts[0].trim().split(":")
                    val endParts = parts[1].trim().split(":")
                    if (startParts.size == 2) {
                        pickerStartHour = startParts[0].toInt().coerceIn(0, 23)
                        pickerStartMinute = startParts[1].toInt().coerceIn(0, 59)
                    }
                    if (endParts.size == 2) {
                        pickerEndHour = endParts[0].toInt().coerceIn(0, 23)
                        pickerEndMinute = endParts[1].toInt().coerceIn(0, 59)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            AppPageTopBar(
                title = "编辑考试",
                scrollBehavior = topAppBarScrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            val selectedName = if (changeToSelect.value) {
                                courseNames.getOrNull(selectedIndex).orEmpty()
                            } else {
                                inputCourseName.value
                            }
                            if (selectedName.isBlank()) {
                                courseNameError = true
                                AppLogger.log("EXAM_EDIT", level = AppLogger.Level.ERROR, message = "课程名为空，保存失败")
                                return@IconButton
                            }
                            val toSave = vm.buildExaminationToSave(
                                courseName = selectedName,
                                datePart = datePartValue.value,
                                timePart = timePartValue.value,
                                examinationPlace = examinationPlaceValue.value,
                                zwh = zwhValue.value,
                                ksbz = ksbzValue.value,
                                courses = courses,
                                existing = editingExamination,
                            )
                            if (toSave != null) {
                                vm.saveExamination(toSave, onSaved = onBack)
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
            if (showDatePicker.value) {
                DatePickerBottomSheet(
                    show = showDatePicker,
                    title = "考试日期",
                    initialDate = pickerDate,
                    onConfirm = { date ->
                        pickerDate = date
                        datePartValue.value = date.toString()
                        showDatePicker.value = false
                    },
                )
            }
            if (showTimePicker.value) {
                TimeRangePickerBottomSheet(
                    show = showTimePicker,
                    title = "考试时间",
                    initialStartHour = pickerStartHour,
                    initialStartMinute = pickerStartMinute,
                    initialEndHour = pickerEndHour,
                    initialEndMinute = pickerEndMinute,
                    onConfirm = { sh, sm, eh, em ->
                        pickerStartHour = sh
                        pickerStartMinute = sm
                        pickerEndHour = eh
                        pickerEndMinute = em
                        timePartValue.value = "${padTwo(sh)}:${padTwo(sm)}-${padTwo(eh)}:${padTwo(em)}"
                        showTimePicker.value = false
                    },
                )
            }
        },
        bottomBar = {
            if (isEdit.value) {
                Button(
                    onClick = {
                        if (examinationId != null) {
                            vm.deleteExamination(examinationId, onDeleted = onBack)
                        }
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    cornerRadius = 20.dp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 25.dp, start = 10.dp, end = 10.dp)
                ) {
                    Text(text = "删除考试", color = MiuixTheme.colorScheme.onPrimary)
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
                                colors = if (courseNameError) TextFieldDefaults.textFieldColors(borderColor = MiuixTheme.colorScheme.error) else TextFieldDefaults.textFieldColors()
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
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    ArrowPreference(
                        title = "考试日期",
                        summary = datePartValue.value.ifBlank { "请选择" },
                        onClick = { showDatePicker.value = true }
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    ArrowPreference(
                        title = "考试时间",
                        summary = timePartValue.value.ifBlank { "请选择" },
                        onClick = { showTimePicker.value = true }
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    TextField(
                        label = "考试地点",
                        value = examinationPlaceValue.value,
                        onValueChange = { examinationPlaceValue.value = it }
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    TextField(
                        label = "座位号",
                        value = zwhValue.value,
                        onValueChange = { zwhValue.value = it }
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                AppCard {
                    TextField(
                        label = "备注",
                        value = ksbzValue.value,
                        onValueChange = { ksbzValue.value = it }
                    )
                }
            }
        }
    }
}
