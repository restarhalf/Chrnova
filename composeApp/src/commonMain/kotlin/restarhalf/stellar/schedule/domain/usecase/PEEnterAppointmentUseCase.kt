package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.PEAppointmentActionResponse
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

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
