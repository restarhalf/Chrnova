package restarhalf.stellar.schedule.data.remote.agent

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AgentAuthPluginTest {

    @Test
    fun loggedIn_agentRequest_autoInjectsXUserId() = runTest {
        var injectedHeader: String? = null
        val client = testClient(TestSessionProvider(userId = "20231234")) { request ->
            injectedHeader = request.headers["x-user-id"]
            respond("ok", HttpStatusCode.OK)
        }

        val response = client.get("https://example.com/api/agent/conversations").bodyAsText()

        assertEquals("ok", response)
        assertEquals("20231234", injectedHeader)
    }

    @Test
    fun loggedOut_agentRequest_blockedBeforeNetwork() = runTest {
        var hitNetwork = false
        val client = testClient(TestSessionProvider(userId = null)) {
            hitNetwork = true
            respond("should not hit", HttpStatusCode.OK)
        }

        assertFailsWith<UserIdentityMissingException> {
            client.get("https://example.com/api/agent/conversations")
        }
        assertTrue(!hitNetwork)
    }

    @Test
    fun nonAgentRequest_notAffected() = runTest {
        var injectedHeader: String? = null
        val client = testClient(TestSessionProvider(userId = null)) { request ->
            injectedHeader = request.headers["x-user-id"]
            respond("ok", HttpStatusCode.OK)
        }

        client.get("https://example.com/api/ping")

        assertEquals(null, injectedHeader)
    }

    private fun testClient(
        provider: AgentAuthSessionProvider,
        handler: MockEngine.Handler,
    ): HttpClient {
        val engine = MockEngine(handler)
        return HttpClient(engine) {
            install(ContentNegotiation) { json() }
            install(AgentAuthPlugin) {
                sessionProvider = provider
            }
        }
    }

    private class TestSessionProvider(
        private val userId: String?,
    ) : AgentAuthSessionProvider {
        override fun userId(): String? = userId
        override fun token(): String? = null
        override fun clearSession() = Unit
        override suspend fun refreshToken(): Boolean = false
    }
}
