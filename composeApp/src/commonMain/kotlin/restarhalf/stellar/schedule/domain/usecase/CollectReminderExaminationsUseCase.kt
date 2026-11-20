package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.policy.distinctExaminations
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class CollectReminderExaminationsUseCase(
    private val fetchExaminations: FetchExaminationsUseCase,
    private val repository: ExaminationRepository,
) {

    data class Result(
        val exams: List<Examination>,
        val remoteFailed: Boolean,
    )

    suspend operator fun invoke(
        semester: String = "",
        nameOrNumber: String = "",
    ): Result {
        val remote =
            runCatching { fetchExaminations(semester = semester, nameOrNumber = nameOrNumber) }
                .onFailure {
                    if (it is CancellationException) throw it
                }
                .getOrNull()

        val local =
            runCatching { repository.getAllExaminationsOnce() }
                .onFailure {
                    if (it is CancellationException) throw it
                }
                .getOrNull()
                .orEmpty()

        return Result(
            exams = (remote.orEmpty() + local).distinctExaminations(),
            remoteFailed = remote == null,
        )
    }
}
