package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.SyncResult

interface SyncPort {
    suspend fun sync(semesterId: String, campusId: String, week: String = "all"): SyncResult
}
