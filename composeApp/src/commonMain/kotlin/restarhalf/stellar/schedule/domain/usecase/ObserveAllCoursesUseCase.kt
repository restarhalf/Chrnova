package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * 观察所有课程用例
 * 
 * 获取所有课程的响应式数据流。
 */
class ObserveAllCoursesUseCase(
    private val courseRepository: CourseRepository,
) {
    /**
     * 观察所有课程
     * 
     * @return 课程列表Flow
     */
    operator fun invoke(): Flow<List<Course>> = courseRepository.observeAllCourses()
}
