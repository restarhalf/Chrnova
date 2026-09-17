package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEAppointmentActionResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 提交体测预约用例
 *
 * 请求体：appointment_id + times_id + enter_date
 */
class PEEnterAppointmentUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    suspend operator fun invoke(
        appointmentId: String,
        timesId: String,
        enterDate: String,
    ): PEAppointmentActionResponse = withSessionRetry(authWorkflow) {
        gateway.enterAppointment(
            appointmentId = appointmentId,
            timesId = timesId,
            enterDate = enterDate,
        )
    }
}
