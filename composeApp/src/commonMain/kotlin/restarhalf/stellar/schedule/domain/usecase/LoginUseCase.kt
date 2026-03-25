package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort

class LoginUseCase(
    private val authWorkflow: AuthWorkflowPort,
) {
    suspend operator fun invoke(
        userNo: String,
        password: String,
        captchaData: String = "",
        codeVal: String = "",
        p: String? = null,
    ) {
        authWorkflow.login(
            userNo = userNo,
            password = password,
            captchaData = captchaData,
            codeVal = codeVal,
            p = p
        )
    }
}
