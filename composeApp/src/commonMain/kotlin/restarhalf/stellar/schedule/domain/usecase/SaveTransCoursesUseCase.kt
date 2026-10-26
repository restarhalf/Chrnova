package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class SaveTransCoursesUseCase(private val repository: CourseRepository) {

    suspend operator fun invoke(
        originCourse: Course,
        overrideCourses: List<Course>,
        clearedWeeks: Collection<Int> = emptyList(),
    ): Boolean {
        if (overrideCourses.isEmpty() && clearedWeeks.isEmpty()) return false

        return runCatching {
            // 原课程本身就是调课记录时，归属键要退回它最初那门课。
            val originKey =
                originCourse.originRemoteKey?.takeIf { it.isNotBlank() } ?: originCourse.remoteKey
            if (originKey.isNotBlank()) {
                val replacedOriginWeeks = overrideCourses.flatMap { it.weeks }.toSet() + clearedWeeks
                // 同一个目标周也要顶掉旧记录，否则换周后会叠出两节课。
                val replacedTargetWeeks = overrideCourses.map { it.targetWeek }.toSet()
                repository
                    .getAllCoursesOnce()
                    .filter { it.type == 2 && it.originRemoteKey == originKey }
                    .filter { old ->
                        old.weeks.any { it in replacedOriginWeeks } ||
                            old.targetWeek in replacedTargetWeeks
                    }
                    .forEach { repository.deleteCourse(it) }
            }
            if (overrideCourses.isNotEmpty()) {
                repository.insertCourses(overrideCourses)
            }
        }
            .onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Course", "保存调课失败", e)
            }
            .isSuccess
    }
}
