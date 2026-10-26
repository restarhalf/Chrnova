package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort

class JwxtLoginUseCase(
    private val authWorkflow: JwxtAuthWorkflowPort,
) {
    
    suspend operator fun invoke(
        userNo: String,
        password: String,
        captchaData: String = "",
        codeVal: String = "",
        p: String? = null,
    ) {
        try {
            authWorkflow.login(
                userNo = userNo,
                password = password,
                captchaData = captchaData,
                codeVal = codeVal,
                p = p
            )
        } catch (e: Exception) {
            AppLogger.log("Auth", "登录失败 userNo=$userNo", e)
            throw e
        }
    }
}
