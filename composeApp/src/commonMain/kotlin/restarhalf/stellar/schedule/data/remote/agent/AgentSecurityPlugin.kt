package restarhalf.stellar.schedule.data.remote.agent

import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore

class AgentAuthSessionProvider(
    private val authStore: JwxtAuthStore,
) : AuthSessionProvider {
    override fun getStableUserId(): String? = authStore.getUserNo()
}

class AgentAuthException(message: String) : IllegalStateException(message)

val AgentSecurityPlugin = createClientPlugin("AgentSecurityPlugin", ::AgentSecurityConfig) {
    val sessionProvider = pluginConfig.sessionProvider
    val telemetry = pluginConfig.telemetry

    onRequest { request, _ ->
        val path = request.url.encodedPath
        if (!path.startsWith(AGENT_API_PATH_PREFIX)) return@onRequest

        val userId = sessionProvider.getStableUserId()
            ?: run {
                telemetry.onUserIdInjection(path, injected = false, hashPrefix = null)
                throw UserIdentityMissingException()
            }

        request.headers.remove(AGENT_USER_ID_HEADER)
        request.headers.append(AGENT_USER_ID_HEADER, userId)
        telemetry.onUserIdInjection(path, injected = true, hashPrefix = AgentIdentityHasher.hashPrefix(userId))
    }

    HttpResponseValidator {
        validateResponse { response ->
            when (response.status) {
                HttpStatusCode.Forbidden -> {
                    telemetry.onForbidden(response.request.url.encodedPath)
                    throw AgentAuthException("403 forbidden")
                }
                HttpStatusCode.Unauthorized -> throw AgentAuthException("401 unauthorized")
                else -> Unit
            }
        }
    }
}

class AgentSecurityConfig {
    lateinit var sessionProvider: AuthSessionProvider
    var telemetry: AgentSecurityTelemetry = NoopAgentSecurityTelemetry
}

