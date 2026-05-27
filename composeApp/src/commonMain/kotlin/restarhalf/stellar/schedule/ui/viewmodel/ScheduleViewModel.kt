package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.core.course.effectiveCoursesForWeek
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.core.text.WeeksFormatter
import restarhalf.stellar.schedule.core.time.AcademicCalendar
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.usecase.BuildScheduleUiStateUseCase
import restarhalf.stellar.schedule.domain.usecase.DeleteCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.InsertCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveShowNonCurrentWeekUseCase
import restarhalf.stellar.schedule.domain.usecase.RefreshCourseRemindersIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.SetShowNonCurrentWeekUseCase
import restarhalf.stellar.schedule.domain.usecase.ShouldAutoSyncAndMarkUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseWithConflictsUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.mapper.DayRenderData
import restarhalf.stellar.schedule.ui.mapper.buildDayRenderData
import kotlin.time.ExperimentalTime

class ScheduleViewModel(
    private val observeShowNonCurrentWeek: ObserveShowNonCurrentWeekUseCase,
    private val setShowNonCurrentWeekUseCase: SetShowNonCurrentWeekUseCase,
    private val observeAllCoursesUseCase: ObserveAllCoursesUseCase,
    private val buildScheduleUiStateUseCase: BuildScheduleUiStateUseCase,
    private val transCourseWithConflicts: TransCourseWithConflictsUseCase,
    private val insertCourseUseCase: InsertCourseUseCase,
    private val deleteCourseUseCase: DeleteCourseUseCase,
    private val shouldAutoSyncAndMark: ShouldAutoSyncAndMarkUseCase,
    private val refreshCourseRemindersIfEnabledUseCase: RefreshCourseRemindersIfEnabledUseCase,
) : ViewModel() {

    data class WeekHeaderUi(
        val days: List<String>,
        val dates: List<String>,
        val todayIndex: Int?,
    )

    data class PageRenderUi(
        val actualWeek: Int,
        val dayRenderData: Map<Int, DayRenderData>,
    )

    data class ScheduleUiState(
        val showNonCurrentWeek: Boolean,
        val transDialogUiState: TransDialogUiState,
        val transConflictUiState: TransConflictUiState,
        val detailSheetUiState: DetailSheetUiState,
    )

    enum class CourseDetailTagStyle {
        LAB,
        TRANS,
        NON_CURRENT,
    }

    data class CourseDetailUi(
        val weekLine: String,
        val locationLine: String,
        val tagText: String?,
        val tagStyle: CourseDetailTagStyle?,
    )

    data class TransDialogUiState(
        val show: Boolean = false,
        val course: Course? = null,
        val targetWeek: Int = 1,
        val originWeek: Int = 1,
        val newClassRoom: String = "",
        val dayOfWeek: Int = 1,
        val startSection: Int = 1,
        val endSection: Int = 2,
    )

    data class TransConflictUiState(
        val show: Boolean = false,
        val conflicts: List<Course> = emptyList(),
        val pendingOverride: Course? = null,
    )

    data class DetailSheetUiState(
        val show: Boolean = false,
        val courses: List<Course> = emptyList(),
    )

    data class TransOperationInput(
        val course: Course,
        val originWeek: Int,
        val targetWeek: Int,
        val newRoom: String,
        val dayOfWeek: Int,
        val startSection: Int,
        val endSection: Int,
    )

    private companion object {
        val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    }

    private val _transDialogUiState = MutableStateFlow(TransDialogUiState())
    private val _transConflictUiState = MutableStateFlow(TransConflictUiState())
    private val _detailSheetUiState = MutableStateFlow(DetailSheetUiState())

    private val _uiState: StateFlow<ScheduleUiState> =
        combine(
            observeShowNonCurrentWeek(),
            _transDialogUiState,
            _transConflictUiState,
            _detailSheetUiState,
        ) { showNonCurrentWeek, transDialogUiState, transConflictUiState, detailSheetUiState ->
            ScheduleUiState(
                showNonCurrentWeek = showNonCurrentWeek,
                transDialogUiState = transDialogUiState,
                transConflictUiState = transConflictUiState,
                detailSheetUiState = detailSheetUiState,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    ScheduleUiState(
                        showNonCurrentWeek = true,
                        transDialogUiState = TransDialogUiState(),
                        transConflictUiState = TransConflictUiState(),
                        detailSheetUiState = DetailSheetUiState(),
                    ),
            )

    val uiState: StateFlow<ScheduleUiState> = _uiState

    fun onShowNonCurrentWeekChanged(show: Boolean) {
        setShowNonCurrentWeekUseCase.invoke(show)
    }

    fun observeAllCourses(): Flow<List<Course>> = observeAllCoursesUseCase()

    suspend fun buildTransCourseAndConflicts(
        originCourse: Course,
        originWeek: Int,
        targetWeek: Int,
        newRoom: String,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
    ): TransCourseWithConflictsUseCase.Result {
        return withContext(AppIoDispatcher) {
            transCourseWithConflicts(
                originCourse = originCourse,
                originWeek = originWeek,
                targetWeek = targetWeek,
                newRoom = newRoom,
                dayOfWeek = dayOfWeek,
                startSection = startSection,
                endSection = endSection
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun buildScheduleUiState(
        campus: Campus,
        totalWeeks: Int,
        termStartMs: Long,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    ): BuildScheduleUiStateUseCase.ScheduleUiState {
        return buildScheduleUiStateUseCase(
            campus = campus,
            totalWeeks = totalWeeks,
            termStartMs = termStartMs,
            nowMs = nowMs
        )
    }

    fun pageToWeek(page: Int, includeWeek0: Boolean): Int {
        return buildScheduleUiStateUseCase.pageToWeek(page = page, includeWeek0 = includeWeek0)
    }

    fun weekToPage(week: Int, includeWeek0: Boolean): Int {
        return buildScheduleUiStateUseCase.weekToPage(week = week, includeWeek0 = includeWeek0)
    }

    fun buildWeekHeaderUi(
        currentWeek: Int,
        detectedDiffDays: Int,
        detectedWeek: Int,
        termStartMs: Long,
        dayCount: Int,
    ): WeekHeaderUi {
        val headerWeek = if (currentWeek == 0) {
            if (detectedDiffDays < 0) -1 else detectedWeek
        } else {
            currentWeek
        }

        val dates = if (headerWeek == -1) {
            AcademicCalendar.getCurrentWeekDates(dayCount = dayCount)
        } else {
            AcademicCalendar.getWeekDates(headerWeek, termStartMs).take(dayCount)
        }

        val todayIndex = if (headerWeek == -1) {
            AcademicCalendar.getTodayIndexInCurrentWeek()
        } else {
            AcademicCalendar.getTodayIndexInWeek(headerWeek, termStartMs)
        }

        return WeekHeaderUi(days = WEEKDAYS, dates = dates, todayIndex = todayIndex)
    }

    fun buildPageRenderUi(
        courses: List<Course>,
        page: Int,
        includeWeek0: Boolean,
        dayCount: Int,
        showNonCurrentWeek: Boolean,
        isDarkMode: Boolean,
        mutedCourseColor: Color,
        mutedTitleColor: Color,
        mutedSubColor: Color,
        yForSection: (Int) -> Dp,
        heightForSections: (Int) -> Dp,
        cellInset: Dp,
        contentCardAlpha: Float,
    ): PageRenderUi {
        val actualWeek = pageToWeek(page = page, includeWeek0 = includeWeek0)
        val effectiveCourses = effectiveCoursesForWeek(all = courses, week = actualWeek)
        val dayRenderData =
            (1..dayCount).associateWith { day ->
                buildDayRenderData(
                    dayCourses = effectiveCourses.filter { it.dayOfWeek == day },
                    page = actualWeek,
                    showNonCurrentWeek = showNonCurrentWeek,
                    isDarkMode = isDarkMode,
                    mutedCourseColor = mutedCourseColor,
                    mutedTitleColor = mutedTitleColor,
                    mutedSubColor = mutedSubColor,
                    yForSection = yForSection,
                    heightForSections = heightForSections,
                    cellInset = cellInset,
                    contentCardAlpha = contentCardAlpha
                )
            }
        return PageRenderUi(actualWeek = actualWeek, dayRenderData = dayRenderData)
    }

    fun openDetailSheet(courses: List<Course>) {
        _detailSheetUiState.value =
            DetailSheetUiState(show = courses.isNotEmpty(), courses = courses)
    }

    fun closeDetailSheet() {
        _detailSheetUiState.value = DetailSheetUiState()
    }

    fun openTransDialog(course: Course, currentWeek: Int) {
        val safeWeek = currentWeek.coerceAtLeast(1)
        _transDialogUiState.value =
            TransDialogUiState(
                show = true,
                course = course,
                targetWeek = safeWeek,
                originWeek = safeWeek,
                newClassRoom = course.location,
                dayOfWeek = course.dayOfWeek.coerceIn(1, 7),
                startSection = course.startSection.coerceIn(1, 12),
                endSection = (course.startSection + course.sectionCount - 1).coerceIn(1, 12),
            )
    }

    fun dismissTransDialog() {
        _transDialogUiState.value = _transDialogUiState.value.copy(show = false)
    }

    fun closeTransDialogAndClear() {
        _transDialogUiState.value = TransDialogUiState()
    }

    fun updateTransTargetWeek(week: Int) {
        _transDialogUiState.value = _transDialogUiState.value.copy(targetWeek = week)
    }

    fun updateTransNewClassRoom(value: String) {
        _transDialogUiState.value = _transDialogUiState.value.copy(newClassRoom = value)
    }

    fun updateTransDayOfWeek(dayOfWeek: Int) {
        _transDialogUiState.value =
            _transDialogUiState.value.copy(dayOfWeek = dayOfWeek.coerceIn(1, 7))
    }

    fun updateTransSectionRange(startSection: Int, endSection: Int) {
        _transDialogUiState.value =
            _transDialogUiState.value.copy(
                startSection = startSection.coerceIn(1, 12),
                endSection = endSection.coerceIn(1, 12)
            )
    }

    fun buildTransOperationInput(): TransOperationInput? {
        val state = _transDialogUiState.value
        val course = state.course ?: return null
        return TransOperationInput(
            course = course,
            originWeek = state.originWeek,
            targetWeek = state.targetWeek,
            newRoom = state.newClassRoom,
            dayOfWeek = state.dayOfWeek,
            startSection = state.startSection,
            endSection = state.endSection
        )
    }

    fun showTransConflict(conflicts: List<Course>, pendingOverride: Course) {
        _transConflictUiState.value =
            TransConflictUiState(
                show = true,
                conflicts = conflicts,
                pendingOverride = pendingOverride
            )
    }

    fun dismissTransConflict(reopenTransDialog: Boolean) {
        _transConflictUiState.value = _transConflictUiState.value.copy(show = false)
        if (reopenTransDialog && _transDialogUiState.value.course != null) {
            _transDialogUiState.value = _transDialogUiState.value.copy(show = true)
        }
    }

    fun clearTransConflict() {
        _transConflictUiState.value = TransConflictUiState()
    }

    fun consumePendingOverride(): Course? {
        val pending = _transConflictUiState.value.pendingOverride
        _transConflictUiState.value = TransConflictUiState()
        return pending
    }

    private fun buildCourseWeekText(course: Course): String {
        val weekText = WeeksFormatter.format(course.weeks)
        if (course.type != 2 || course.targetWeek <= 0) return weekText

        val targetWeekText = "第${course.targetWeek}周"
        return if (weekText.isBlank()) {
            targetWeekText
        } else {
            "$targetWeekText（原$weekText）"
        }
    }

    fun buildCourseDetailUi(
        course: Course,
        currentWeek: Int,
        timetable: List<TimetableSlot>,
    ): CourseDetailUi {
        val weekItem = buildCourseWeekText(course)
        val startSection = course.startSection
        val endSection = course.startSection + course.sectionCount - 1
        val startTime = timetable.getOrNull(startSection - 1)?.start ?: "--"
        val endTime = timetable.getOrNull(endSection - 1)?.end ?: "--"

        val (tagText, tagStyle) =
            when {
                course.type == 1 && course.weeks.contains(currentWeek) && currentWeek != 0 ->
                    "实验课" to CourseDetailTagStyle.LAB

                course.type == 2 && course.targetWeek == currentWeek && currentWeek != 0 ->
                    "调课" to CourseDetailTagStyle.TRANS

                !isCourseActiveInWeek(
                    course,
                    currentWeek
                ) -> "非本周" to CourseDetailTagStyle.NON_CURRENT

                else -> null to null
            }

        return CourseDetailUi(
            weekLine = "$weekItem   |   第$startSection-$endSection 节($startTime-$endTime)",
            locationLine = "${course.location}   |   ${course.teacher}",
            tagText = tagText,
            tagStyle = tagStyle
        )
    }

    fun insertCourse(course: Course) {
        viewModelScope.launch {
            withContext(AppIoDispatcher) { insertCourseUseCase(course) }
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            withContext(AppIoDispatcher) { deleteCourseUseCase(course) }
        }
    }

    fun saveTransCourse(overrideCourse: Course) {
        insertCourse(overrideCourse)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun shouldAutoSync(): Boolean {
        return withContext(AppIoDispatcher) {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            shouldAutoSyncAndMark(nowMs = now)
        }
    }

    suspend fun refreshCourseRemindersIfEnabled(
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ) {
        withContext(AppIoDispatcher) {
            refreshCourseRemindersIfEnabledUseCase(
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks
            )
        }
    }
}
