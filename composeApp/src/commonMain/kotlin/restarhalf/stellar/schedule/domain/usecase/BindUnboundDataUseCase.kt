package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 绑定旧数据用例
 *
 * 将数据库中未绑定学号的课程和考试数据绑定到当前登录用户。
 * 同时修复考试表中无效的学期ID（空或"manual"）。
 * 用于数据库升级后，将旧数据归属到当前账号。
 */
class BindUnboundDataUseCase(
    private val auth: AuthPort,
    private val courseRepository: CourseRepository,
    private val examinationRepository: ExaminationRepository,
    private val academic: AcademicPort,
) {
    /**
     * 执行绑定
     *
     * 获取当前登录用户的学号，将所有 userNo 为空的课程和考试绑定到该学号。
     * 同时修复考试表中无效的学期ID。
     * 如果未登录则跳过。
     */
    suspend operator fun invoke() {
        val userNo = auth.observeProfile().firstOrNull()?.userNo
        if (userNo.isNullOrBlank()) return

        courseRepository.bindUnboundCourses(userNo)
        examinationRepository.bindUnboundExaminations(userNo)
        fixInvalidSemesterIds()
    }

    /**
     * 修复考试表中无效的学期ID
     *
     * 将 semesterId 为空或 "manual" 的考试更新为当前学期ID。
     */
    private suspend fun fixInvalidSemesterIds() {
        try {
            val currentSemesterId = academic.fetchCurrentTermId()
            if (currentSemesterId.isNotBlank()) {
                examinationRepository.fixInvalidSemesterIds(currentSemesterId)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AppLogger.log("Bind", "修复考试学期ID失败", e)
        }
    }
}
