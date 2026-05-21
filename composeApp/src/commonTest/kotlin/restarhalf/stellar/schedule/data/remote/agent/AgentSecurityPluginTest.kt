package restarhalf.stellar.schedule.data.remote.agent

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AgentSecurityPluginTest {
    @Test
    fun injectsHeaderForAgentRequestWhenLoggedIn() {
        var capturedHeader: String? = null
        val client = HttpClient(MockEngine { req ->
            capturedHeader = req.headers[AGENT_USER_ID_HEADER]
            respond("{}", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }) {
            install(AgentSecurityPlugin) {
                sessionProvider = object : AuthSessionProvider {
                    override fun getStableUserId(): String = "2020123456"
                }
            }
        }

        client.get("https://example.com/api/agent/conversations")
        assertEquals("2020123456", capturedHeader)
    }

    @Test
    fun blocksAgentRequestWhenLoggedOut() {
        val client = HttpClient(MockEngine {
            respond("{}", HttpStatusCode.OK)
        }) {
            install(AgentSecurityPlugin) {
                sessionProvider = object : AuthSessionProvider {
                    override fun getStableUserId(): String? = null
                }
            }
        }

        assertFailsWith<UserIdentityMissingException> {
            client.get("https://example.com/api/agent/conversations")
        }
    }

    @Test
    fun nonAgentRequestIsNotAffected() {
        var capturedHeader: String? = "unset"
        val client = HttpClient(MockEngine { req ->
            capturedHeader = req.headers[AGENT_USER_ID_HEADER]
            respond("{}", HttpStatusCode.OK)
        }) {
            install(AgentSecurityPlugin) {
                sessionProvider = object : AuthSessionProvider {
                    override fun getStableUserId(): String? = null
                }
            }
        }

        client.get("https://example.com/api/grades")
        assertEquals(null, capturedHeader)
    }
}
