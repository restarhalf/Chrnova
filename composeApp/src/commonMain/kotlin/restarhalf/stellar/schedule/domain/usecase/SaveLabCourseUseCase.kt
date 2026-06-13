package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * 保存实验课用例
 * 
 * 将实验课数据保存到本地数据库。
 */
class SaveLabCourseUseCase(private val courseRepository: CourseRepository) {
    /**
     * 保存实验课
     * 
     * @param course 实验课数据
     * @return 保存后的课程ID
     */
    suspend operator fun invoke(course: Course): Long {
        return courseRepository.insertCourse(course)
    }
}
