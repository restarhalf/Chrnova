package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * 一次性获取所有课程用例
 * 
 * 从本地数据库一次性获取所有课程数据。
 */
class GetAllCoursesOnceUseCase(
    private val courseRepository: CourseRepository,
) {
    /**
     * 获取所有课程
     * 
     * @return 课程列表
     */
    suspend operator fun invoke(): List<Course> = courseRepository.getAllCoursesOnce()
}
