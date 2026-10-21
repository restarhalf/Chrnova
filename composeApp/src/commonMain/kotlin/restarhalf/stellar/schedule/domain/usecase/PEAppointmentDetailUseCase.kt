package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.PEAppointmentDetailResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentTimesResponse
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

class PEAppointmentDetailUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    suspend fun detail(
        appointmentId: String,
        appointmentStatus: String,
    ): PEAppointmentDetailResponse = withSessionRetry(authWorkflow) {
        gateway.getAppointmentDetail(
            appointmentId = appointmentId,
            appointmentStatus = appointmentStatus,
        )
    }

    suspend fun times(
        appointmentId: String,
        appointmentDate: String,
    ): PEAppointmentTimesResponse = withSessionRetry(authWorkflow) {
        gateway.getAppointmentTimes(
            appointmentId = appointmentId,
            appointmentDate = appointmentDate,
        )
    }
}
