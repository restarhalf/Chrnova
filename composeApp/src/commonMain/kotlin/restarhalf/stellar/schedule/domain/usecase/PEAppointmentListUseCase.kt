package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEAppointmentListResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 学生预约列表用例
 */
class PEAppointmentListUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    /**
     * @param type "1"=我的预约，"2"=可预约
     */
    suspend operator fun invoke(
        type: String,
        pageNum: Int = 1,
        pageSize: Int = 20,
    ): PEAppointmentListResponse = withSessionRetry(authWorkflow) {
        gateway.getAppointments(type = type, pageNum = pageNum, pageSize = pageSize)
    }
}
