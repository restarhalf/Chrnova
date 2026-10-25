package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.PEFreeActionResponse
import restarhalf.stellar.schedule.domain.model.PEFreeApplyDetailResponse
import restarhalf.stellar.schedule.domain.model.PEFreeApplyListResponse
import restarhalf.stellar.schedule.domain.model.PEFreeSchoolYearResponse
import restarhalf.stellar.schedule.domain.policy.distinctFreeApplies
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

class PEFreeApplyUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    suspend fun list(): PEFreeApplyListResponse = withSessionRetry(authWorkflow) {
        val response = gateway.getFreeApplyList()
        val items = response.data.distinctFreeApplies()
        if (items.size == response.data.size) response else response.copy(data = items)
    }

    suspend fun detail(applyId: String): PEFreeApplyDetailResponse = withSessionRetry(authWorkflow) {
        gateway.getFreeApplyDetail(applyId)
    }

    suspend fun schoolYears(): PEFreeSchoolYearResponse = withSessionRetry(authWorkflow) {
        gateway.getFreeSchoolYears()
    }

    suspend fun apply(
        stdNumber: String,
        schoolYear: String,
        freeApplyType: String,
        attachments: List<String>,
    ): PEFreeActionResponse = withSessionRetry(authWorkflow) {
        gateway.submitFreeApply(
            stdNumber = stdNumber,
            schoolYear = schoolYear,
            freeApplyType = freeApplyType,
            attachments = attachments,
        )
    }

    suspend fun upload(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): PEFreeActionResponse = withSessionRetry(authWorkflow) {
        gateway.uploadPeFile(fileName = fileName, mimeType = mimeType, bytes = bytes)
    }

    suspend fun downloadAtt(attId: String): ByteArray = withSessionRetry(authWorkflow) {
        gateway.downloadPeFile(attId)
    }

    fun attPreviewUrl(attId: String): String = gateway.peFileDownloadUrl(attId)
}
