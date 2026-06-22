package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 绑定旧数据用例
 *
 * 将数据库中未绑定学号的课程和考试数据绑定到当前登录用户。
 * 用于数据库升级后，将旧数据归属到当前账号。
 */
class BindUnboundDataUseCase(
    private val auth: AuthPort,
    private val courseRepository: CourseRepository,
    private val examinationRepository: ExaminationRepository,
) {
    /**
     * 执行绑定
     *
     * 获取当前登录用户的学号，将所有 userNo 为空的课程和考试绑定到该学号。
     * 如果未登录则跳过。
     */
    suspend operator fun invoke() {
        val userNo = auth.observeProfile().firstOrNull()?.userNo
        if (userNo.isNullOrBlank()) return

        courseRepository.bindUnboundCourses(userNo)
        examinationRepository.bindUnboundExaminations(userNo)
    }
}
