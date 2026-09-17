package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEAppointmentDetailResponse
import restarhalf.stellar.schedule.data.remote.PEAppointmentTimesResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 预约详情 / 某日时段查询用例
 */
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
