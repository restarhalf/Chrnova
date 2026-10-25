package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.repository.GradeRepository


class GetGradesByUserNoUseCase(private val repository: GradeRepository) {

    suspend operator fun invoke(userNo: String): List<GradeCourse> =
        repository.getAllGradesByUserNo(userNo)
}
