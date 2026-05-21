package restarhalf.stellar.schedule.data.remote.agent

import kotlin.experimental.and

internal const val AGENT_USER_ID_HEADER = "x-user-id"
internal const val AGENT_API_PATH_PREFIX = "/api/agent/"

class UserIdentityMissingException : IllegalStateException("User identity is required for Agent API")

interface AuthSessionProvider {
    fun getStableUserId(): String?
}

internal object AgentIdentityHasher {
    fun hashPrefix(value: String): String {
        val bytes = value.encodeToByteArray()
        var hash = 0x811c9dc5.toInt()
        bytes.forEach { byte ->
            hash = hash xor (byte and 0xff.toByte()).toInt()
            hash *= 0x01000193
        }
        return hash.toUInt().toString(16).padStart(8, '0').take(8)
    }
}

internal interface AgentSecurityTelemetry {
    fun onUserIdInjection(path: String, injected: Boolean, hashPrefix: String?)
    fun onForbidden(path: String)
}

internal object NoopAgentSecurityTelemetry : AgentSecurityTelemetry {
    override fun onUserIdInjection(path: String, injected: Boolean, hashPrefix: String?) {
        println("AgentSecurity injection path=$path injected=$injected hashPrefix=${hashPrefix ?: "none"}")
    }

    override fun onForbidden(path: String) {
        println("AgentSecurity forbidden path=$path")
    }
}
