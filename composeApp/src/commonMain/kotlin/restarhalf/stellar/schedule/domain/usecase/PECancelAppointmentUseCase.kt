package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEAppointmentActionResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 取消体测预约用例
 */
class PECancelAppointmentUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    suspend operator fun invoke(temporaryId: String): PEAppointmentActionResponse =
        withSessionRetry(authWorkflow) {
            gateway.cancelAppointment(temporaryId)
        }
}
