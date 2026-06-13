package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * 插入课程用例
 * 
 * 将课程数据插入到本地数据库。
 */
class InsertCourseUseCase(
    private val courseRepository: CourseRepository,
) {
    /**
     * 插入课程
     * 
     * @param course 课程数据
     * @return 插入后的课程ID
     */
    suspend operator fun invoke(course: Course): Long = courseRepository.insertCourse(course)
}
