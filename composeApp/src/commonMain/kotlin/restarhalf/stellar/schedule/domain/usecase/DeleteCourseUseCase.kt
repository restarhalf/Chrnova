package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * 删除课程用例
 * 
 * 从本地数据库删除指定课程。
 */
class DeleteCourseUseCase(private val courseRepository: CourseRepository) {
    /**
     * 删除课程
     * 
     * @param course 要删除的课程
     */
    suspend operator fun invoke(course: Course) {
        courseRepository.deleteCourse(course)
    }
}
