package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.CancellationException
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

    
    override fun logout() {
        authStore.clearSession()
    }

    
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
