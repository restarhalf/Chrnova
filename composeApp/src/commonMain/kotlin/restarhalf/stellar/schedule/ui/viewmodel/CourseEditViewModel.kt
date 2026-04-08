package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.usecase.DeleteCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveCourseByIdUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveLabCourseUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher

class CourseEditViewModel(
    private val observeAllCoursesUseCase: ObserveAllCoursesUseCase,
    private val observeCourseByIdUseCase: ObserveCourseByIdUseCase,
    private val saveLabCourseUseCase: SaveLabCourseUseCase,
    private val deleteCourseUseCase: DeleteCourseUseCase,
) : ViewModel() {

    data class EditingFormState(
        val isEdit: Boolean,
        val selectedIndex: Int,
        val classRoom: String,
        val dayOfWeek: Int,
        val startSection: Int,
        val endSection: Int,
        val selectedWeeks: Set<Int>,
    )

    fun buildCourseNames(courses: List<Course>): List<String> {
        return courses.filter { it.type == 0 }.map { it.name }.distinct()
    }

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
            type = 1
        )
    }

    fun observeEditingCourse(courseId: Long?): Flow<Course?> {
        return observeCourseByIdUseCase(courseId ?: -1)
    }

    fun buildEditingFormState(
        courseId: Long?,
        editingCourse: Course?,
        courseNames: List<String>,
    ): EditingFormState {
        if (courseId == null || editingCourse == null) {
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

    fun toggleWeek(selectedWeeks: Set<Int>, week: Int): Set<Int> {
        return if (selectedWeeks.contains(week)) selectedWeeks - week else selectedWeeks + week
    }

    fun sectionSummary(startSection: Int, endSection: Int): String {
        return "第${startSection}-${endSection}节"
    }

    fun observeAllCourses(): Flow<List<Course>> = observeAllCoursesUseCase()

    fun observeCourseById(id: Long): Flow<Course?> = observeCourseByIdUseCase(id)

    fun saveLabCourse(course: Course, onSaved: () -> Unit) {
        viewModelScope.launch {
            withContext(AppIoDispatcher) { saveLabCourseUseCase(course) }
            onSaved()
        }
    }

    fun deleteCourse(course: Course, onDeleted: () -> Unit) {
        viewModelScope.launch {
            withContext(AppIoDispatcher) { deleteCourseUseCase(course) }
            onDeleted()
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
