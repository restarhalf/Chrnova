package restarhalf.stellar.schedule.data.remote

import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.port.JwxtPasswordEncryptionPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JwxtClientTest {

    private val store = JwxtAuthStore(MapSettings())
    private val encryption = mock<JwxtPasswordEncryptionPort>(MockMode.autofill).apply {
        every { encryptPasswordForLogin(any()) } returns "encrypted"
    }
    private val capturedTokens = mutableListOf<String?>()
    private val capturedCookies = mutableListOf<String?>()

    private fun jwxtClient(body: String, setCookie: String? = null): JwxtClient {
        val httpClient = HttpClient(MockEngine) {
            install(ContentNegotiation) { json() }
            install(JwxtAuthPlugin) { authStore = store }
            engine {
                addHandler { request ->
                    capturedTokens += request.headers["token"]
                    capturedCookies += request.headers["Cookie"]
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headers {
                            append(HttpHeaders.ContentType, "application/json")
                            if (setCookie != null) append(HttpHeaders.SetCookie, setCookie)
                        },
                    )
                }
            }
        }
        return JwxtClient(
            httpClient = httpClient,
            json = Json { ignoreUnknownKeys = true },
            authStore = store,
            passwordEncryption = encryption,
        )
    }

    @Test
    fun `登录成功才写入token和Cookie`() = runTest {
        val client = jwxtClient(
            body = """{"code":1,"data":{"userNo":"2023001","token":"new-token"}}""",
            setCookie = "SERVERID=abc; Path=/",
        )

        client.login("2023001", "password")

        assertEquals("new-token", store.getToken())
        assertEquals("SERVERID=abc", store.getServerIdCookie())
    }

    @Test
    fun `响应解析失败不留下半更新状态`() = runTest {
        store.setToken("old-token")
        store.setServerIdCookie("SERVERID=old")
        val client = jwxtClient(body = "not-json", setCookie = "SERVERID=new; Path=/")

        assertFailsWith<SerializationException> { client.login("2023001", "password") }

        assertEquals("old-token", store.getToken())
        assertEquals("SERVERID=old", store.getServerIdCookie())
    }

    @Test
    fun `登录请求不携带旧token和Cookie`() = runTest {
        store.setToken("old-token")
        store.setServerIdCookie("SERVERID=old")
        val client = jwxtClient(
            body = """{"code":1,"data":{"userNo":"2023001","token":"new-token"}}""",
        )

        client.login("2023001", "password")

        assertNull(capturedTokens.single())
        assertNull(capturedCookies.single())
    }
}
