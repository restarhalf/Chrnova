package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.policy.distinctExaminations
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 排考试提醒时用的考试清单：教务拉回来的 + 本地库里手动添加的。
 *
 * 之前排提醒只看教务返回，自己加的考试（source = manual，只在本地库里）永远不会被排上。
 * 现在两边合并，教务拉不到（断网、没登录）就退化成只用本地库，不至于一条都排不出来。
 */
class CollectReminderExaminationsUseCase(
    private val fetchExaminations: FetchExaminationsUseCase,
    private val repository: ExaminationRepository,
) {

    data class Result(
        val exams: List<Examination>,
        /** 教务这次没拉到；此时 [exams] 只有本地库里的，可能是空的 */
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
                    AppLogger.log("Reminder", "拉取考试失败，改用本地库", it)
                }
                .getOrNull()

        val local =
            runCatching { repository.getAllExaminationsOnce() }
                .onFailure {
                    if (it is CancellationException) throw it
                    AppLogger.log("Reminder", "读取本地考试失败", it)
                }
                .getOrNull()
                .orEmpty()

        // 远程的放前面：同一场考试以刚拉到的为准
        return Result(
            exams = (remote.orEmpty() + local).distinctExaminations(),
            remoteFailed = remote == null,
        )
    }
}
