package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.repository.CourseRepository

/** 缩略态精简预览的候选项：当前学期课表里的课程名与上课地点（各自去重）。 */
data class ClassroomLabelPreview(
    val courseNames: List<String> = emptyList(),
    val classrooms: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = courseNames.isEmpty() && classrooms.isEmpty()
}

class LoadClassroomLabelPreviewUseCase(
    private val courseRepository: CourseRepository,
) {
    suspend operator fun invoke(): ClassroomLabelPreview {
        val courses = courseRepository.getAllCoursesOnce()
        return ClassroomLabelPreview(
            courseNames = courses.map { it.name }.filter { it.isNotBlank() }.distinct().sorted(),
            classrooms = courses.map { it.location }.filter { it.isNotBlank() }.distinct().sorted(),
        )
    }
}
