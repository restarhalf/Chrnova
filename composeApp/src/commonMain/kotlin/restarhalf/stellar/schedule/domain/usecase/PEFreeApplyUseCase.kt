package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEFreeActionResponse
import restarhalf.stellar.schedule.data.remote.PEFreeApplyDetailResponse
import restarhalf.stellar.schedule.data.remote.PEFreeApplyListResponse
import restarhalf.stellar.schedule.data.remote.PEFreeSchoolYearResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 学生免测申请（gymFreeManager）用例
 */
class PEFreeApplyUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    suspend fun list(): PEFreeApplyListResponse = withSessionRetry(authWorkflow) {
        gateway.getFreeApplyList()
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

    /** 附件预览/看图 URL，走 Coil，与公告图片同一套缓存 */
    fun attPreviewUrl(attId: String): String = gateway.peFileDownloadUrl(attId)
}
