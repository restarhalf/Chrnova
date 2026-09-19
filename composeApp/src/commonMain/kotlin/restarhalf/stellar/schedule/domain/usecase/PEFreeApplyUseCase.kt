package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEFreeActionResponse
import restarhalf.stellar.schedule.data.remote.PEFreeApplyListResponse
import restarhalf.stellar.schedule.data.remote.PEFreeSchoolYearResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 学生免测（gymFreeManager）用例
 */
class PEFreeApplyUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    suspend fun list(): PEFreeApplyListResponse = withSessionRetry(authWorkflow) {
        gateway.getFreeApplyList()
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
}
