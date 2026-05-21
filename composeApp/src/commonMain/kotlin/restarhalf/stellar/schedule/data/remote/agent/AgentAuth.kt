package restarhalf.stellar.schedule.data.remote.agent

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import kotlin.experimental.and

private const val AGENT_PATH_PREFIX = "/api/agent/"
private const val USER_ID_HEADER = "x-user-id"

class UserIdentityMissingException : IllegalStateException("请先登录后使用智能助手")

class AgentForbiddenException : IllegalStateException("当前账号无权限访问智能助手")

interface AgentAuthSessionProvider {
    fun userId(): String?
    fun clearSession()
    suspend fun refreshToken(): Boolean
}

fun interface AgentAuthTelemetry {
    fun onIdentityHeaderInjection(success: Boolean, userIdHashPrefix: String?)
}

class AgentAuthPluginConfig {
    var sessionProvider: AgentAuthSessionProvider? = null
    var telemetry: AgentAuthTelemetry = AgentAuthTelemetry { _, _ -> }
}

val AgentAuthPlugin = createClientPlugin("AgentAuthPlugin", ::AgentAuthPluginConfig) {
    val sessionProvider = pluginConfig.sessionProvider ?: return@createClientPlugin
    val telemetry = pluginConfig.telemetry

    onRequest { request, _ ->
        if (!request.url.toString().contains(AGENT_PATH_PREFIX)) return@onRequest
        val userId = sessionProvider.userId().orEmpty().trim()
        if (userId.isBlank()) {
            telemetry.onIdentityHeaderInjection(false, null)
            throw UserIdentityMissingException()
        }
        request.enforceUserHeader(userId)
        telemetry.onIdentityHeaderInjection(true, userId.hashPrefix())
    }

    onResponse { response ->
        if (!response.request.url.toString().contains(AGENT_PATH_PREFIX)) return@onResponse
        when (response.status) {
            HttpStatusCode.Unauthorized -> {
                val refreshed = sessionProvider.refreshToken()
                if (!refreshed) {
                    sessionProvider.clearSession()
                    throw UserIdentityMissingException()
                }
            }
            HttpStatusCode.Forbidden -> {
                throw AgentForbiddenException()
            }
            else -> Unit
        }
    }
}

private fun HttpRequestBuilder.enforceUserHeader(userId: String) {
    headers.remove(USER_ID_HEADER)
    header(USER_ID_HEADER, userId)
}

private fun String.hashPrefix(): String {
    val bytes = encodeToByteArray()
    var hash = 0x811c9dc5.toInt()
    for (b in bytes) {
        hash = hash xor (b and 0xff.toByte()).toInt()
        hash *= 0x01000193
    }
    return hash.toUInt().toString(16).padStart(8, '0').take(8)
}
