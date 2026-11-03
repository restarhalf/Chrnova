package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restarhalf.stellar.schedule.core.error.CredentialsRejectedException
import restarhalf.stellar.schedule.core.error.isCredentialRejection
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
        val oldUserId = authStore.getUserId().orEmpty()
        val resp = gateway.login(
            username = username,
            password = password
        )

        if (resp.status != "PASS") {
            val message = resp.message.ifBlank { "登录失败" }
            if (isCredentialRejection(message)) throw CredentialsRejectedException(message)
            throw IllegalStateException(message)
        }

        val token = resp.token?.takeIf { it.isNotBlank() }
        val userId = resp.userId?.takeIf { it.isNotBlank() }
        if (token == null || userId == null) {
            authStore.clearSession()
            throw IllegalStateException("登录响应缺少 Token 或用户标识，请重新登录")
        }

        val newUsername = username.trim()
        if (oldUsername.isNotBlank() && newUsername.isNotBlank() && oldUsername != newUsername) {
            repository.clearUser(oldUserId)
        }

        authStore.setLastUsername(newUsername)
        authStore.setCredentials(username, password)
    }

    override suspend fun logout() {
        authMutex.withLock {
            val userId = authStore.getUserId().orEmpty()
            authStore.clear()
            repository.clearUser(userId)
        }
    }

    override suspend fun refreshSession() {
        val tokenBefore = authStore.getToken()

        authMutex.withLock {
            val credentials = authStore.getCredentials()
            if (credentials == null) {
                AppLogger.log("PEAuth", "刷新会话失败，没有保存的凭据，清理本地会话")
                authStore.clearSession()
                return
            }

            val currentToken = authStore.getToken()
            if (currentToken != null && currentToken != tokenBefore) return

            try {
                performLogin(username = credentials.first, password = credentials.second)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isCredentialRejection(e.message.orEmpty())) {
                    AppLogger.log("PEAuth", "刷新会话失败，账号或密码错误，退出登录", e)
                    authStore.clearSession()
                    authStore.clearCredentials()
                    return
                }
                AppLogger.log("PEAuth", "刷新会话失败，恢复旧Token", e)
                if (tokenBefore != null) authStore.setToken(tokenBefore)
            }
        }
    }
}
