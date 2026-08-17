package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.PEAuthStore
import restarhalf.stellar.schedule.data.repository.PERepository
import restarhalf.stellar.schedule.data.repository.PERoomRepository
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 体育系统认证工作流端口实现类
 *
 * 实现PEAuthWorkflowPort接口，负责体育系统认证流程的完整处理。
 * 包括登录、登出、会话刷新等操作。
 *
 * @param repository 体育数据仓库
 * @param authStore 体育系统认证存储
 * @param roomRepository 体育本地缓存仓库
 */
class PEAuthWorkflowPortImpl(
    private val repository: PERepository,
    private val authStore: PEAuthStore,
    private val roomRepository: PERoomRepository,
) : PEAuthWorkflowPort {

    /**
     * 确保用户已登录
     *
     * 如果已有有效令牌，直接返回；否则使用保存的凭据登录。
     *
     * @throws IllegalStateException 未保存凭据时抛出
     */
    override suspend fun ensureLoggedIn() {
        val token = authStore.getToken().orEmpty()
        if (token.isNotBlank()) return

        val (username, password) = authStore.getCredentials()
            ?: throw IllegalStateException("请先登录")
        login(username = username, password = password)
    }

    /**
     * 用户登录
     *
     * 如果登录用户与之前不同，会清除本地体育缓存数据。
     *
     * @param username 用户名
     * @param password 密码
     * @throws IllegalStateException 登录失败时抛出
     */
    override suspend fun login(
        username: String,
        password: String,
    ) {
        val oldUsername = authStore.getLastUsername().orEmpty().trim()
        val resp = repository.login(
            username = username,
            password = password
        )

        if (resp.status != "PASS") {
            throw IllegalStateException(resp.message.ifBlank { "登录失败" })
        }

        val token = authStore.getToken().orEmpty()
        if (token.isBlank()) {
            throw IllegalStateException("登录成功但未获取到 Token")
        }

        // 如果用户切换，清除旧用户的体育缓存数据
        val newUsername = username.trim()
        if (oldUsername.isNotBlank() && newUsername.isNotBlank() && oldUsername != newUsername) {
            roomRepository.clearAll()
        }

        authStore.setLastUsername(newUsername)
        authStore.setCredentials(username, password)
    }

    /** 用户登出，清除会话 */
    override fun logout() {
        authStore.clearSession()
    }

    /** 刷新会话，重新登录 */
    override suspend fun refreshSession() {
        val (username, password) = authStore.getCredentials() ?: return
        val oldToken = authStore.getToken()
        runCatching { login(username = username, password = password) }
            .onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("PEAuth", "刷新会话失败，恢复旧Token", e)
                if (oldToken != null) authStore.setToken(oldToken)
            }
    }
}
