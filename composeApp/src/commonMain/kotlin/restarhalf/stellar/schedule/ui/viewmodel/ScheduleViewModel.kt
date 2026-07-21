package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.core.course.effectiveCoursesForWeek
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.text.WeeksFormatter
import restarhalf.stellar.schedule.core.time.AcademicCalendar
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.usecase.BuildScheduleUiStateUseCase
import restarhalf.stellar.schedule.domain.usecase.RefreshCourseRemindersIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseWithConflictsUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.mapper.DayRenderData
import restarhalf.stellar.schedule.ui.mapper.buildDayRenderData
import kotlin.time.ExperimentalTime

/**
 * 课程表ViewModel
 * 
 * 管理课程表页面的UI状态，包括：
 * - 周次切换和页面渲染
 * - 课程详情展示
 * - 调课操作
 * - 非本周课程显示设置
 */
class ScheduleViewModel(
    private val settings: SettingsPort,
    private val courseRepository: CourseRepository,
    private val buildScheduleUiStateUseCase: BuildScheduleUiStateUseCase,
    private val transCourseWithConflicts: TransCourseWithConflictsUseCase,
    private val refreshCourseRemindersIfEnabledUseCase: RefreshCourseRemindersIfEnabledUseCase,
) : ViewModel() {

    /**
     * 周次头部UI
     * 
     * @param days 星期名称列表（周一至周日）
     * @param dates 对应日期列表（如"9/01"）
     * @param todayIndex 今日在该周中的索引，不在该周时为null
     */
    @Stable
    data class WeekHeaderUi(
        val days: ImmutableList<String>,
        val dates: ImmutableList<String>,
        val todayIndex: Int?,
    )

    /**
     * 页面渲染UI
     * 
     * @param actualWeek 实际周次
     * @param dayRenderData 每天的渲染数据映射
     */
    @Stable
    data class PageRenderUi(
        val actualWeek: Int,
        val dayRenderData: ImmutableMap<Int, DayRenderData>,
    )

    /**
     * 课程表UI状态
     * 
     * @param showNonCurrentWeek 是否显示非本周课程
     * @param transDialogUiState 调课对话框状态
     * @param transConflictUiState 调课冲突状态
     * @param detailSheetUiState 课程详情弹窗状态
     */
    @Immutable
    data class ScheduleUiState(
        val showNonCurrentWeek: Boolean,
        val transDialogUiState: TransDialogUiState,
        val transConflictUiState: TransConflictUiState,
        val detailSheetUiState: DetailSheetUiState,
    )

    /**
     * 课程详情标签样式枚举
     */
    enum class CourseDetailTagStyle {
        /** 实验课 */
        LAB,
        /** 调课 */
        TRANS,
        /** 非本周课程 */
        NON_CURRENT,
    }

    /**
     * 课程详情UI
     * 
     * @param weekLine 周次和节次信息行
     * @param locationLine 地点和教师信息行
     * @param tagText 标签文本（如"实验课"、"调课"）
     * @param tagStyle 标签样式
     */
    @Immutable
    data class CourseDetailUi(
        val weekLine: String,
        val locationLine: String,
        val tagText: String?,
        val tagStyle: CourseDetailTagStyle?,
    )

    /**
     * 调课对话框UI状态
     * 
     * @param show 是否显示
     * @param course 要调课的课程
     * @param targetWeek 目标周次
     * @param originWeek 原始周次
     * @param newClassRoom 新教室
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     */
    @Immutable
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

    /**
     * 调课冲突UI状态
     * 
     * @param show 是否显示
     * @param conflicts 冲突的课程列表
     * @param pendingOverride 待覆盖的课程
     */
    @Stable
    data class TransConflictUiState(
        val show: Boolean = false,
        val conflicts: ImmutableList<Course> = persistentListOf(),
        val pendingOverride: Course? = null,
    )

    /**
     * 课程详情弹窗UI状态
     * 
     * @param show 是否显示
     * @param courses 该时间段的课程列表
     */
    @Stable
    data class DetailSheetUiState(
        val show: Boolean = false,
        val courses: ImmutableList<Course> = persistentListOf(),
    )

    /**
     * 调课操作输入参数
     * 
     * @param course 原始课程
     * @param originWeek 原始周次
     * @param targetWeek 目标周次
     * @param newRoom 新教室
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     */
    @Immutable
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
        val WEEKDAYS = ClockTime.weekDays
    }

    private val _transDialogUiState = MutableStateFlow(TransDialogUiState())
    private val _transConflictUiState = MutableStateFlow(TransConflictUiState())
    private val _detailSheetUiState = MutableStateFlow(DetailSheetUiState())

    private val _uiState: StateFlow<ScheduleUiState> =
        combine(
            settings.observeShowNonCurrentWeek(),
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

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<ScheduleUiState> = _uiState

    /** 观察所有课程变化 */
    val allCourses: StateFlow<List<Course>> = courseRepository.observeAllCourses()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * 构建调课课程并检查冲突
     * 
     * @param originCourse 原始课程
     * @param originWeek 原始周次
     * @param targetWeek 目标周次
     * @param newRoom 新教室
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     * @return 调课结果，包含是否有冲突
     */
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

    /**
     * 构建课程表UI状态
     * 
     * @param campus 当前校区
     * @param totalWeeks 学期总周数
     * @param termStartMs 学期开始时间戳
     * @param nowMs 当前时间戳
     * @return 课程表UI状态
     */
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

    /**
     * 将页面索引转换为周次
     * 
     * @param page 页面索引
     * @param includeWeek0 是否包含第0周
     * @return 周次
     */
    fun pageToWeek(page: Int, includeWeek0: Boolean): Int {
        return buildScheduleUiStateUseCase.pageToWeek(page = page, includeWeek0 = includeWeek0)
    }

    /**
     * 将周次转换为页面索引
     * 
     * @param week 周次
     * @param includeWeek0 是否包含第0周
     * @return 页面索引
     */
    fun weekToPage(week: Int, includeWeek0: Boolean): Int {
        return buildScheduleUiStateUseCase.weekToPage(week = week, includeWeek0 = includeWeek0)
    }

    /**
     * 构建周次头部UI
     * 
     * @param currentWeek 当前周次
     * @param detectedDiffDays 检测到的与学期开始的天数差
     * @param detectedWeek 检测到的周次
     * @param termStartMs 学期开始时间戳
     * @param dayCount 显示天数
     * @return 周次头部UI
     */
    fun buildWeekHeaderUi(
        currentWeek: Int,
        detectedDiffDays: Int,
        detectedWeek: Int,
        termStartMs: Long,
        dayCount: Int,
    ): WeekHeaderUi {
        val headerWeek = if (currentWeek == 0) {
            if (detectedDiffDays < 0 || detectedWeek == 0) -1 else detectedWeek
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

        return WeekHeaderUi(days = WEEKDAYS.toPersistentList(), dates = dates.toPersistentList(), todayIndex = todayIndex)
    }

    /**
     * 构建页面渲染UI
     * 
     * @param courses 所有课程列表
     * @param page 页面索引
     * @param includeWeek0 是否包含第0周
     * @param dayCount 显示天数
     * @param showNonCurrentWeek 是否显示非本周课程
     * @param isDarkMode 是否为深色模式
     * @param mutedCourseColor 非当前周课程颜色
     * @param mutedTitleColor 非当前周标题颜色
     * @param mutedSubColor 非当前周副标题颜色
     * @param yForSection 计算节次Y坐标的函数
     * @param heightForSections 计算节次高度的函数
     * @param cellInset 单元格内边距
     * @return 页面渲染UI
     */
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
        effectiveCourses: List<Course>? = null,
    ): PageRenderUi {
        val actualWeek = pageToWeek(page = page, includeWeek0 = includeWeek0)
        val weekCourses =
            effectiveCourses ?: effectiveCoursesForWeek(all = courses, week = actualWeek)
        val dayRenderData =
            (1..dayCount).associateWith { day ->
                buildDayRenderData(
                    dayCourses = weekCourses.filter { it.dayOfWeek == day },
                    page = actualWeek,
                    showNonCurrentWeek = showNonCurrentWeek,
                    isDarkMode = isDarkMode,
                    mutedCourseColor = mutedCourseColor,
                    mutedTitleColor = mutedTitleColor,
                    mutedSubColor = mutedSubColor,
                    yForSection = yForSection,
                    heightForSections = heightForSections,
                    cellInset = cellInset,
                )
            }
        return PageRenderUi(actualWeek = actualWeek, dayRenderData = dayRenderData.toPersistentMap())
    }

    /**
     * 打开课程详情弹窗
     * 
     * @param courses 该时间段的课程列表
     */
    fun openDetailSheet(courses: List<Course>) {
        _detailSheetUiState.value =
            DetailSheetUiState(show = courses.isNotEmpty(), courses = courses.toPersistentList())
    }

    /** 关闭课程详情弹窗 */
    fun closeDetailSheet() {
        _detailSheetUiState.value = DetailSheetUiState()
    }

    /**
     * 打开调课对话框
     * 
     * @param course 要调课的课程
     * @param currentWeek 当前周次
     */
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

    /** 隐藏调课对话框（不清除数据） */
    fun dismissTransDialog() {
        _transDialogUiState.value = _transDialogUiState.value.copy(show = false)
    }

    /** 关闭调课对话框并清除数据 */
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

    /**
     * 构建调课操作输入参数
     * 
     * @return 调课操作输入参数，如果课程为空返回null
     */
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

    /**
     * 显示调课冲突
     * 
     * @param conflicts 冲突的课程列表
     * @param pendingOverride 待覆盖的课程
     */
    fun showTransConflict(conflicts: List<Course>, pendingOverride: Course) {
        _transConflictUiState.value =
            TransConflictUiState(
                show = true,
                conflicts = conflicts.toPersistentList(),
                pendingOverride = pendingOverride
            )
    }

    /**
     * 隐藏调课冲突对话框
     * 
     * @param reopenTransDialog 是否重新打开调课对话框
     */
    fun dismissTransConflict(reopenTransDialog: Boolean) {
        _transConflictUiState.value = _transConflictUiState.value.copy(show = false)
        if (reopenTransDialog && _transDialogUiState.value.course != null) {
            _transDialogUiState.value = _transDialogUiState.value.copy(show = true)
        }
    }

    /** 清除调课冲突状态 */
    fun clearTransConflict() {
        _transConflictUiState.value = TransConflictUiState()
    }

    /**
     * 消费待覆盖的课程
     * 
     * @return 待覆盖的课程，如果没有返回null
     */
    fun consumePendingOverride(): Course? {
        val pending = _transConflictUiState.value.pendingOverride
        _transConflictUiState.value = TransConflictUiState()
        return pending
    }

    /**
     * 构建课程周次文本
     * 
     * @param course 课程
     * @return 格式化的周次文本
     */
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

    /**
     * 构建课程详情UI
     * 
     * @param course 课程
     * @param currentWeek 当前周次
     * @param timetable 时间槽配置
     * @return 课程详情UI
     */
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

    /**
     * 插入课程
     * 
     * @param course 要插入的课程
     */
    fun insertCourse(course: Course) {
        viewModelScope.launch {
            runCatching { withContext(AppIoDispatcher) { courseRepository.insertCourse(course) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Schedule", "插入课程失败", e)
                }
        }
    }

    /**
     * 删除课程
     * 
     * @param course 要删除的课程
     */
    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            runCatching { withContext(AppIoDispatcher) { courseRepository.deleteCourse(course) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Schedule", "删除课程失败", e)
                }
        }
    }

    /**
     * 保存调课后的课程
     * 
     * @param overrideCourse 覆盖的课程
     */
    fun saveTransCourse(overrideCourse: Course) {
        insertCourse(overrideCourse)
    }

    /**
     * 检查是否需要自动同步
     * 
     * @return 如果距离上次同步超过24小时返回true
     */
    @OptIn(ExperimentalTime::class)
    suspend fun shouldAutoSync(): Boolean {
        return withContext(AppIoDispatcher) {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val result = settings.shouldAutoSyncAndMark(nowMs = now)
            result
        }
    }

    /**
     * 如果启用则刷新课程提醒
     * 
     * @param campus 当前校区
     * @param termStartMs 学期开始时间戳
     * @param totalWeeks 学期总周数
     */
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
