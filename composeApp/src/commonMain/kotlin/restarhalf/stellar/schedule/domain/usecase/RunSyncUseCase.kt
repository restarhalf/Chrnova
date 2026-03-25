package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.ReminderSchedulerPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.port.TimetablePort

class RunSyncUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
    private val timetable: TimetablePort,
    private val settings: SettingsPort,
    private val sync: SyncPort,
    private val reminderScheduler: ReminderSchedulerPort,
) {

    suspend operator fun invoke(): SyncResult {
        authWorkflow.ensureLoggedIn()

        val selectedTerm = settings.observeSelectedTerm().first()
        val semesterId =
            if (selectedTerm.isNotBlank()) selectedTerm else academic.fetchCurrentTermId()

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

        val firstAttempt =
            runCatching { sync.sync(semesterId = semesterId, campusId = campus.id, week = "all") }
        val result =
            if (firstAttempt.isSuccess) {
                firstAttempt.getOrThrow()
            } else {
                authWorkflow.logout()
                authWorkflow.ensureLoggedIn()
                sync.sync(semesterId = semesterId, campusId = campus.id, week = "all")
            }

        reminderScheduler.scheduleNow()

        return result.copy(campusName = campus.name)
    }

    private fun isCampusNameMatch(local: Campus, remoteName: String): Boolean {
        val name = remoteName.trim()
        return when (local) {
            Campus.Development -> name.contains("开发区")
            Campus.Jinshitan -> name.contains("金石滩")
        }
    }
}
