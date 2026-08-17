package restarhalf.stellar.schedule.domain.port

/**
 * 体育系统认证工作流端口接口
 *
 * 定义体育系统认证流程的抽象接口，包括登录、登出、会话刷新等。
 */
interface PEAuthWorkflowPort {
    /**
     * 确保用户已登录
     *
     * 如果当前未登录，会自动尝试使用已保存的凭据登录。
     */
    suspend fun ensureLoggedIn()

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     */
    suspend fun login(
        username: String,
        password: String,
    )

    /** 用户登出，清除登录会话 */
    fun logout()

    /** 刷新当前会话，延长登录状态有效期 */
    suspend fun refreshSession()
}
