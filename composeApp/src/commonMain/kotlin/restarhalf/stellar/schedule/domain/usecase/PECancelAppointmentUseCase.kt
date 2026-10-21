package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.PEAppointmentActionResponse
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

class PECancelAppointmentUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    suspend operator fun invoke(temporaryId: String): PEAppointmentActionResponse =
        withSessionRetry(authWorkflow) {
            gateway.cancelAppointment(temporaryId)
        }
}
