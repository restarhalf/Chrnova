package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort

/**
 * 登录用例
 * 
 * 封装用户登录的业务逻辑，调用认证工作流端口执行登录操作。
 */
class LoginUseCase(
    private val authWorkflow: AuthWorkflowPort,
) {
    /**
     * 执行登录
     * 
     * @param userNo 学号
     * @param password 密码
     * @param captchaData 验证码数据
     * @param codeVal 用户输入的验证码
     * @param p 加密参数
     */
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
