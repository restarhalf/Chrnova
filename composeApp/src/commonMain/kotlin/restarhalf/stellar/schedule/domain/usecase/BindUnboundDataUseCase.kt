package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository


class BindUnboundDataUseCase(
    private val auth: JwxtAuthPort,
    private val courseRepository: CourseRepository,
    private val examinationRepository: ExaminationRepository,
    private val academic: AcademicPort,
) {
    
    suspend operator fun invoke() {
        val userNo = auth.observeProfile().firstOrNull()?.userNo
        if (userNo.isNullOrBlank()) return

        courseRepository.bindUnboundCourses(userNo)
        examinationRepository.bindUnboundExaminations(userNo)
        fixInvalidSemesterIds()
    }

    
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
