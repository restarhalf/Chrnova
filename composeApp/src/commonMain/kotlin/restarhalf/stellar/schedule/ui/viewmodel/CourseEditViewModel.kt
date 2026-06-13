package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.usecase.DeleteCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveCourseByIdUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveLabCourseUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 课程编辑ViewModel
 * 
 * 管理实验课编辑页面的UI状态，包括：
 * - 课程列表和名称
 * - 编辑表单状态
 * - 冲突检测
 * - 保存和删除操作
 */
class CourseEditViewModel(
    private val observeAllCoursesUseCase: ObserveAllCoursesUseCase,
    private val observeCourseByIdUseCase: ObserveCourseByIdUseCase,
    private val saveLabCourseUseCase: SaveLabCourseUseCase,
    private val deleteCourseUseCase: DeleteCourseUseCase,
) : ViewModel() {

    /**
     * 课程编辑UI状态
     * 
     * @param courses 所有课程列表
     * @param courseNames 课程名称列表（用于下拉选择）
     */
    data class CourseEditUiState(
        val courses: List<Course>,
        val courseNames: List<String>,
    )

    /**
     * 编辑表单状态
     * 
     * @param isEdit 是否为编辑模式（false表示新建）
     * @param selectedIndex 选中的课程名称索引
     * @param classRoom 教室
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     * @param selectedWeeks 选中的周次集合
     */
    data class EditingFormState(
        val isEdit: Boolean,
        val selectedIndex: Int,
        val classRoom: String,
        val dayOfWeek: Int,
        val startSection: Int,
        val endSection: Int,
        val selectedWeeks: Set<Int>,
    )

    private val _uiState: StateFlow<CourseEditUiState> =
        observeAllCoursesUseCase()
            .combine(MutableStateFlow(Unit)) { courses, _ ->
                CourseEditUiState(courses = courses, courseNames = buildCourseNames(courses))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CourseEditUiState(courses = emptyList(), courseNames = emptyList()),
            )

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<CourseEditUiState> = _uiState

    /**
     * 构建课程名称列表
     * 
     * @param courses 所有课程列表
     * @return 去重的普通课程名称列表
     */
    fun buildCourseNames(courses: List<Course>): List<String> {
        return courses.filter { it.type == 0 }.map { it.name }.distinct()
    }

    /**
     * 将星期数转换为中文文本
     * 
     * @param dayOfWeek 星期数（1-7）
     * @return 中文星期文本
     */
    fun weekdayText(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> "周一"
        }
    }

    /**
     * 构建禁用的周次集合
     * 
     * 检测在同一时间段有冲突的周次，这些周次不能选择。
     * 
     * @param courses 所有课程列表
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     * @param courseId 当前课程ID（编辑时）
     * @param editingCourseId 正在编辑的课程ID
     * @return 禁用的周次集合
     */
    fun buildDisabledWeeks(
        courses: List<Course>,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
        courseId: Long?,
        editingCourseId: Long?,
    ): Set<Int> {
        val selfId = courseId ?: editingCourseId
        val result = linkedSetOf<Int>()
        courses
            .asSequence()
            .filter { c -> selfId == null || c.id != selfId }
            .filter { c -> overlapsInDayAndSection(c, dayOfWeek, startSection, endSection) }
            .forEach { c -> result.addAll(c.weeks) }
        return result
    }

    /**
     * 构建要保存的实验课
     * 
     * @param selectedName 课程名称
     * @param selectedWeeks 选中的周次
     * @param classRoom 教室
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     * @param existing 已存在的课程（编辑时）
     * @param courses 所有课程列表
     * @return 构建的课程对象，如果参数无效返回null
     */
    fun buildLabCourseToSave(
        selectedName: String,
        selectedWeeks: Set<Int>,
        classRoom: String,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
        existing: Course?,
        courses: List<Course>,
    ): Course? {
        if (selectedName.isBlank()) return null
        val weeks = selectedWeeks.sorted()
        if (weeks.isEmpty()) return null

        val sectionCount = (endSection - startSection + 1).coerceAtLeast(1)
        // 从同步的课程中获取教师信息
        val teacherFromSyncedCourse =
            courses
                .firstOrNull { it.type == 0 && it.name == selectedName && it.teacher.isNotBlank() }
                ?.teacher
                .orEmpty()
        val semesterId =
            existing?.semesterId
                ?: courses.firstOrNull { it.type == 0 && it.name == selectedName }?.semesterId
                .orEmpty()

        return Course(
            id = existing?.id ?: 0,
            name = selectedName,
            semesterId = semesterId,
            location = classRoom,
            teacher = teacherFromSyncedCourse.ifBlank { existing?.teacher.orEmpty() },
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            sectionCount = sectionCount,
            weeks = weeks,
            color = existing?.color ?: "",
            type = 1  // 实验课类型
        )
    }

    /**
     * 观察正在编辑的课程
     * 
     * @param courseId 课程ID
     * @return 课程Flow
     */
    fun observeEditingCourse(courseId: Long?): Flow<Course?> {
        return observeCourseByIdUseCase(courseId ?: -1)
    }

    /**
     * 构建编辑表单状态
     * 
     * @param courseId 课程ID
     * @param editingCourse 正在编辑的课程
     * @param courseNames 课程名称列表
     * @return 编辑表单状态
     */
    fun buildEditingFormState(
        courseId: Long?,
        editingCourse: Course?,
        courseNames: List<String>,
    ): EditingFormState {
        if (courseId == null || editingCourse == null) {
            // 新建模式
            return EditingFormState(
                isEdit = false,
                selectedIndex = 0,
                classRoom = "",
                dayOfWeek = 1,
                startSection = 1,
                endSection = 2,
                selectedWeeks = emptySet()
            )
        }

        // 编辑模式，填充已有数据
        val selectedIndex = courseNames.indexOf(editingCourse.name).takeIf { it >= 0 } ?: 0
        return EditingFormState(
            isEdit = true,
            selectedIndex = selectedIndex,
            classRoom = editingCourse.location,
            dayOfWeek = editingCourse.dayOfWeek,
            startSection = editingCourse.startSection,
            endSection =
                (editingCourse.startSection + editingCourse.sectionCount - 1)
                    .coerceAtLeast(editingCourse.startSection),
            selectedWeeks = editingCourse.weeks.toSet()
        )
    }

    /**
     * 切换周次选中状态
     * 
     * @param selectedWeeks 当前选中的周次集合
     * @param week 要切换的周次
     * @return 更新后的选中周次集合
     */
    fun toggleWeek(selectedWeeks: Set<Int>, week: Int): Set<Int> {
        return if (selectedWeeks.contains(week)) selectedWeeks - week else selectedWeeks + week
    }

    /**
     * 构建节次摘要文本
     * 
     * @param startSection 开始节次
     * @param endSection 结束节次
     * @return 格式化的节次文本
     */
    fun sectionSummary(startSection: Int, endSection: Int): String {
        return "第${startSection}-${endSection}节"
    }

    /** 观察所有课程 */
    fun observeAllCourses(): Flow<List<Course>> = observeAllCoursesUseCase()

    /**
     * 观察指定ID的课程
     * 
     * @param id 课程ID
     * @return 课程Flow
     */
    fun observeCourseById(id: Long): Flow<Course?> = observeCourseByIdUseCase(id)

    /**
     * 保存实验课
     * 
     * @param course 课程数据
     * @param onSaved 保存完成回调
     */
    fun saveLabCourse(course: Course, onSaved: () -> Unit) {
        viewModelScope.launch {
            withContext(AppIoDispatcher) { saveLabCourseUseCase(course) }
            onSaved()
        }
    }

    /**
     * 删除课程
     * 
     * @param course 课程数据
     * @param onDeleted 删除完成回调
     */
    fun deleteCourse(course: Course, onDeleted: () -> Unit) {
        viewModelScope.launch {
            withContext(AppIoDispatcher) { deleteCourseUseCase(course) }
            onDeleted()
        }
    }

    /**
     * 检查课程是否在同一时间段重叠
     * 
     * @param other 另一门课程
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     * @return 是否重叠
     */
    private fun overlapsInDayAndSection(
        other: Course,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
    ): Boolean {
        if (other.dayOfWeek != dayOfWeek) return false
        val otherStart = other.startSection
        val otherEnd = other.startSection + other.sectionCount - 1
        return maxOf(startSection, otherStart) <= minOf(endSection, otherEnd)
    }
}
