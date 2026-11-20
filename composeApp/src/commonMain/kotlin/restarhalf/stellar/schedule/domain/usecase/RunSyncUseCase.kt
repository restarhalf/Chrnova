package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.error.ParseFailureException
import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.RemoteCampus
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.port.TimetablePort

class RunSyncUseCase(
    private val authWorkflow: JwxtAuthWorkflowPort,
    private val academic: AcademicPort,
    private val timetable: TimetablePort,
    private val settings: TermSettingsPort,
    private val sync: SyncPort,
    private val rescheduleAllReminders: RescheduleAllRemindersUseCase,
) {

    suspend operator fun invoke(): SyncResult {
        authWorkflow.ensureLoggedIn()

        // token 过期可能在 fetchCampuses 等前置请求上就暴露，刷新会话的重试必须包住整段同步，
        // 不能只包住最后的 sync 请求。
        val result =
            try {
                runSync()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e.isNetworkError()) throw e
                if (e is ParseFailureException) throw e
                authWorkflow.refreshSession()
                runSync()
            }

        // 课表/考试刚更新：按新数据整批重排（取消旧闹钟 → 重排，幂等自愈）
        rescheduleAllReminders()

        return result
    }

    private suspend fun runSync(): SyncResult {
        val selectedTerm = settings.observeSelectedTerm().first()
        val semesterId =
            selectedTerm.ifBlank { academic.fetchCurrentTermId() }

        val localCampus = timetable.getCampus()

        val campuses = academic.fetchCampuses()
        if (campuses.isEmpty()) {
            throw IllegalStateException("校区列表为空")
        }

        val campus =
            campuses.firstOrNull { isCampusNameMatch(local = localCampus, remoteName = it.name) }
                ?: campuses.firstOrNull { it.isDefault }
                ?: campuses.firstOrNull()

        if (campus == null || campus.id.isBlank() || campus.name.isBlank()) {
            throw IllegalStateException("获取校区失败")
        }

        val result = syncWithRetry(semesterId, campus, campuses)

        val termStartMs = sync.fetchTermStartDate(semesterId = semesterId, campusId = campus.id)
        if (termStartMs != null) {
            timetable.setTermStartMs(termStartMs)
        }

        val teachingWeekTotal = academic.fetchTeachingWeekTotal()
        if (teachingWeekTotal > 0) {
            timetable.setTotalWeeks(teachingWeekTotal)
        }

        return result
    }

    private suspend fun syncWithRetry(
        semesterId: String,
        campus: RemoteCampus,
        allCampuses: List<RemoteCampus>,
    ): SyncResult {
        val result = doSync(semesterId, campus)

        if (result.inserted == 0 && allCampuses.size > 1) {
            val fallback = allCampuses.firstOrNull { it.id != campus.id && it.id.isNotBlank() }
            if (fallback != null) {
                val fallbackResult = doSync(semesterId, fallback)
                if (fallbackResult.inserted > 0) {
                    val targetCampus = matchLocalCampus(fallback.name)
                    if (targetCampus != null) {
                        timetable.setCampus(targetCampus)
                    }
                    return fallbackResult.copy(campusName = fallback.name)
                }
            }
        }

        return result.copy(campusName = campus.name)
    }

    private suspend fun doSync(
        semesterId: String,
        campus: RemoteCampus,
    ): SyncResult =
        sync.sync(semesterId = semesterId, campusId = campus.id, week = "all")

    private fun matchLocalCampus(remoteName: String): Campus? {
        val name = remoteName.trim()
        return when {
            name.contains("开发区") -> Campus.Development
            name.contains("金石滩") -> Campus.Jinshitan
            else -> null
        }
    }

    private fun isCampusNameMatch(local: Campus, remoteName: String): Boolean {
        val name = remoteName.trim()
        return when (local) {
            Campus.Development -> name.contains("开发区")
            Campus.Jinshitan -> name.contains("金石滩")
        }
    }
}
