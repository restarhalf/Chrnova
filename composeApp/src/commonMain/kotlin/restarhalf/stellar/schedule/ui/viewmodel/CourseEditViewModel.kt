package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.domain.policy.buildCourseNames
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.platform.AppIoDispatcher


class CourseEditViewModel(
    private val courseRepository: CourseRepository,
    private val auth: JwxtAuthPort,
) : ViewModel() {

    
    private val _saving = MutableStateFlow(false)

    
    val isSaving: StateFlow<Boolean> = _saving.asStateFlow()

    
    @Stable
    data class CourseEditUiState(
        val courses: ImmutableList<Course>,
        val courseNames: ImmutableList<String>,
    )

    
    @Stable
    data class EditingFormState(
        val isEdit: Boolean,
        val selectedIndex: Int,
        val classRoom: String,
        val teacher: String,
        val dayOfWeek: Int,
        val startSection: Int,
        val endSection: Int,
        val selectedWeeks: Set<Int>,
    )

    private val _uiState: StateFlow<CourseEditUiState> =
        courseRepository.observeAllCourses()
            .map { courses ->
                CourseEditUiState(courses = courses.toPersistentList(), courseNames = buildCourseNames(courses).toPersistentList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CourseEditUiState(courses = persistentListOf(), courseNames = persistentListOf()),
            )

    
    val uiState: StateFlow<CourseEditUiState> = _uiState

    
    private val userNo: StateFlow<String> = auth.observeProfile()
        .map { it.userNo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "",
        )

    
    fun weekdayText(dayOfWeek: Int): String = ClockTime.weekdayText(dayOfWeek)

    
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

    
    fun buildLabCourseToSave(
        selectedName: String,
        selectedWeeks: Set<Int>,
        classRoom: String,
        teacher: String,
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

        val teacherFromSyncedCourse =
            courses
                .firstOrNull { it.type == 0 && it.name == selectedName && it.teacher.isNotBlank() }
                ?.teacher
                .orEmpty()
        val semesterId =
            existing?.semesterId
                ?: (courses.firstOrNull { it.type == 0 && it.name == selectedName }
                    ?: courses.firstOrNull { it.type == 0 && it.semesterId.isNotBlank() })
                    ?.semesterId
                    .orEmpty()

        return Course(
            id = existing?.id ?: 0,
            name = selectedName,
            semesterId = semesterId,
            location = classRoom,
            teacher = teacher.ifBlank { teacherFromSyncedCourse.ifBlank { existing?.teacher.orEmpty() } },
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            sectionCount = sectionCount,
            weeks = weeks,
            color = existing?.color ?: "",
            type = 1,
            userNo = existing?.userNo?.takeIf { it.isNotBlank() } ?: userNo.value,
        )
    }

    
    fun observeEditingCourse(courseId: Long?): Flow<Course?> {
        return courseRepository.observeCourseById(courseId ?: -1)
    }

    
    fun buildEditingFormState(
        courseId: Long?,
        editingCourse: Course?,
        courseNames: List<String>,
        initialDayOfWeek: Int = 1,
        initialStartSection: Int = 1,
        initialSelectedWeek: Int = 1,
    ): EditingFormState {
        if (courseId == null || editingCourse == null) {

            return EditingFormState(
                isEdit = false,
                selectedIndex = 0,
                classRoom = "",
                teacher = "",
                dayOfWeek = initialDayOfWeek.coerceIn(1, 7),
                startSection = initialStartSection.coerceIn(1, 12),
                endSection = initialStartSection.coerceIn(1, 12),
                selectedWeeks = setOf(initialSelectedWeek.coerceAtLeast(1))
            )
        }


        val selectedIndex = courseNames.indexOf(editingCourse.name).takeIf { it >= 0 } ?: 0
        return EditingFormState(
            isEdit = true,
            selectedIndex = selectedIndex,
            classRoom = editingCourse.location,
            teacher = editingCourse.teacher,
            dayOfWeek = editingCourse.dayOfWeek,
            startSection = editingCourse.startSection,
            endSection =
                (editingCourse.startSection + editingCourse.sectionCount - 1)
                    .coerceAtLeast(editingCourse.startSection),
            selectedWeeks = editingCourse.weeks.toSet()
        )
    }

    
    fun toggleWeek(selectedWeeks: Set<Int>, week: Int): Set<Int> {
        return if (selectedWeeks.contains(week)) selectedWeeks - week else selectedWeeks + week
    }

    
    fun sectionSummary(startSection: Int, endSection: Int): String {
        return "第${startSection}-${endSection}节"
    }

    
    fun saveLabCourse(course: Course, onResult: (Boolean) -> Unit) {
        if (!_saving.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val saved = runCatching { withContext(AppIoDispatcher) { courseRepository.insertCourse(course) } }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        AppLogger.log("CourseEdit", "保存实验课失败", e)
                    }
                    .isSuccess
                onResult(saved)
            } finally {
                _saving.value = false
            }
        }
    }

    
    fun deleteCourse(course: Course, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val deleted = runCatching { withContext(AppIoDispatcher) { courseRepository.deleteCourse(course) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("CourseEdit", "删除课程失败", e)
                }
                .isSuccess
            onResult(deleted)
        }
    }

    
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
