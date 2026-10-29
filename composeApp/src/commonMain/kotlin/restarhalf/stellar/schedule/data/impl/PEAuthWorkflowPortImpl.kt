package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.PEAuthStore
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.repository.PERepository
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

class PEAuthWorkflowPortImpl(
    private val gateway: PEGateway,
    private val authStore: PEAuthStore,
    private val repository: PERepository,
) : PEAuthWorkflowPort {

    private val authMutex = Mutex()

    override suspend fun ensureLoggedIn() {
        val token = authStore.getToken().orEmpty()
        if (token.isNotBlank()) return

        val (username, password) = authStore.getCredentials()
            ?: throw IllegalStateException("请先登录")
        login(username = username, password = password)
    }

    override suspend fun login(
        username: String,
        password: String,
    ) {
        authMutex.withLock {
            performLogin(username = username, password = password)
        }
    }

    private suspend fun performLogin(
        username: String,
        password: String,
    ) {
        val oldUsername = authStore.getLastUsername().orEmpty().trim()
        val resp = gateway.login(
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

        val newUsername = username.trim()
        if (oldUsername.isNotBlank() && newUsername.isNotBlank() && oldUsername != newUsername) {
            repository.clearAll()
        }

        authStore.setLastUsername(newUsername)
        authStore.setCredentials(username, password)
    }

    override suspend fun logout() {
        authMutex.withLock {
            authStore.clear()
            repository.clearAll()
        }
    }

    override suspend fun refreshSession() {
        val tokenBefore = authStore.getToken()

        authMutex.withLock {
            // 等待锁期间可能已退出登录，凭据要以加锁后的状态为准
            val (username, password) = authStore.getCredentials() ?: return

            // 已有其他刷新更新了 token，直接复用，避免并发重复登录
            val currentToken = authStore.getToken()
            if (currentToken != null && currentToken != tokenBefore) return

            val oldToken = authStore.getToken()
            try {
                performLogin(username = username, password = password)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.log("PEAuth", "刷新会话失败，恢复旧Token", e)
                if (oldToken != null) authStore.setToken(oldToken)
            }
        }
    }
}
