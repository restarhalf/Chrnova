package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.ScheduleSnapshot

interface ScheduleSnapshotPort {

    suspend fun load(): ScheduleSnapshot
}
