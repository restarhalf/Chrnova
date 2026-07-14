package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.repository.GradeRepository

class GetLocalGradesByUserNoUseCase(
    private val gradeRepository: GradeRepository,
) {
    suspend operator fun invoke(userNo: String): List<GradeCourse> =
        gradeRepository.getAllGradesByUserNo(userNo)
}
