package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.PEAppointmentListResponse
import restarhalf.stellar.schedule.domain.policy.distinctAppointments
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
        val response = gateway.getAppointments(type = type, pageNum = pageNum, pageSize = pageSize)
        val data = response.data ?: return@withSessionRetry response
        val items = data.dataList.distinctAppointments()
        if (items.size == data.dataList.size) response else response.copy(data = data.copy(dataList = items))
    }
}
