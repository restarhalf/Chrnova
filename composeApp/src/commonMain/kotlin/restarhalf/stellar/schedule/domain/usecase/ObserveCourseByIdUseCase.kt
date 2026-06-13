package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * 根据ID观察课程用例
 * 
 * 获取指定课程的响应式数据流。
 */
class ObserveCourseByIdUseCase(
    private val courseRepository: CourseRepository,
) {
    /**
     * 根据ID观察课程
     * 
     * @param id 课程ID
     * @return 课程Flow
     */
    operator fun invoke(id: Long): Flow<Course?> = courseRepository.observeCourseById(id)
}
