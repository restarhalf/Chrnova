package restarhalf.stellar.schedule.domain.port

/**
 * 认证工作流端口接口
 * 
 * 定义用户认证流程的抽象接口，包括登录、登出、会话刷新等。
 */
interface AuthWorkflowPort {
    /**
     * 确保用户已登录
     * 
     * 如果当前未登录，会自动尝试使用已保存的凭据登录。
     */
    suspend fun ensureLoggedIn()

    /**
     * 用户登录
     * 
     * @param userNo 学号
     * @param password 密码
     * @param captchaData 验证码数据（Base64编码的图片）
     * @param codeVal 用户输入的验证码
     * @param p 加密参数（可选）
     */
    suspend fun login(
        userNo: String,
        password: String,
        captchaData: String = "",
        codeVal: String = "",
        p: String? = null,
    )

    /** 用户登出，清除凭据和会话 */
    fun logout()

    /** 刷新当前会话，延长登录状态有效期 */
    suspend fun refreshSession()
}
