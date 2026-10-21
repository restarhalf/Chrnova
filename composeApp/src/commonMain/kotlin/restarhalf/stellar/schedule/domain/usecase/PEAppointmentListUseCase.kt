package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.PEAppointmentListResponse
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

class PEAppointmentListUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    suspend operator fun invoke(
        type: String,
        pageNum: Int = 1,
        pageSize: Int = 20,
    ): PEAppointmentListResponse = withSessionRetry(authWorkflow) {
        gateway.getAppointments(type = type, pageNum = pageNum, pageSize = pageSize)
    }
}
