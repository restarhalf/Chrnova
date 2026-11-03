package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restarhalf.stellar.schedule.core.error.CredentialsRejectedException
import restarhalf.stellar.schedule.core.error.isCredentialRejection
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort

class JwxtAuthWorkflowPortImpl(
    private val gateway: JwxtGateway,
    private val authStore: JwxtAuthStore,
) : JwxtAuthWorkflowPort {

    private val authMutex = Mutex()

    override suspend fun ensureLoggedIn() {
        val token = authStore.getToken().orEmpty()
        if (token.isNotBlank()) return

        val (userNo, password) = authStore.getCredentials()
            ?: throw IllegalStateException("请先登录")
        login(userNo = userNo, password = password)
    }

    override suspend fun login(
        userNo: String,
        password: String,
        captchaData: String,
        codeVal: String,
        p: String?,
    ) {
        authMutex.withLock {
            performLogin(
                userNo = userNo,
                password = password,
                captchaData = captchaData,
                codeVal = codeVal,
                p = p,
                startNewSession = true,
            )
        }
    }

    private suspend fun performLogin(
        userNo: String,
        password: String,
        captchaData: String = "",
        codeVal: String = "",
        p: String? = null,
        startNewSession: Boolean,
    ) {
        val generation = authStore.getSessionGeneration()
        val resp = gateway.login(
            userNo = userNo,
            password = password,
            captchaData = captchaData,
            codeVal = codeVal,
            p = p
        )

        if (resp.code != 1) {
            val message = resp.messageOrEmpty().ifBlank { "登录失败" }
            if (isCredentialRejection(message)) throw CredentialsRejectedException(message)
            throw IllegalStateException(message)
        }

        if (authStore.getSessionGeneration() != generation) {
            authStore.clearToken()
            throw IllegalStateException("登录已取消")
        }

        val token = authStore.getToken().orEmpty()
        if (token.isBlank()) {
            throw IllegalStateException("登录成功但未获取到 Token")
        }

        val newUserNo = resp.data?.userNo.orEmpty().trim().ifBlank { userNo.trim() }
        authStore.setLastUserNo(newUserNo)

        authStore.setProfile(
            name = resp.data?.name,
            userNo = resp.data?.userNo,
            clsName = resp.data?.clsName,
            academyName = resp.data?.academyName
        )

        authStore.setCredentials(userNo, password)

        if (authStore.getSessionGeneration() != generation) {
            authStore.clear()
            throw IllegalStateException("登录已取消")
        }

        if (startNewSession) {
            authStore.advanceSessionGeneration()
        }
    }

    override suspend fun refreshSession() {
        val tokenBefore = authStore.getToken()

        authMutex.withLock {
            val (userNo, password) = authStore.getCredentials() ?: return

            val currentToken = authStore.getToken()
            if (currentToken != null && currentToken != tokenBefore) return

            val generation = authStore.getSessionGeneration()
            try {
                performLogin(
                    userNo = userNo,
                    password = password,
                    startNewSession = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (authStore.getSessionGeneration() != generation) return
                if (isCredentialRejection(e.message.orEmpty())) {
                    AppLogger.log("Auth", "刷新会话失败，账号或密码错误，退出登录", e)
                    authStore.clear()
                    return
                }
                AppLogger.log("Auth", "刷新会话失败，恢复旧Token", e)
                if (tokenBefore != null) authStore.setToken(tokenBefore)
            }
        }
    }
}
