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
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.port.PEPasswordEncryptionPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PEClientLoginTest {

    private val store = PEAuthStore(MapSettings())
    private val encryption = mock<PEPasswordEncryptionPort>(MockMode.autofill).apply {
        every { encryptPasswordForPELogin(any()) } returns "encrypted"
        every { generatePESign(any()) } returns "sign"
    }

    private fun client(body: String): PEClient {
        val httpClient = HttpClient(MockEngine) {
            install(ContentNegotiation) { json() }
            engine {
                addHandler {
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headers {
                            append(HttpHeaders.ContentType, "application/json")
                        },
                    )
                }
            }
        }
        return PEClient(
            httpClient = httpClient,
            json = Json { ignoreUnknownKeys = true },
            authStore = store,
            passwordEncryption = encryption,
        )
    }

    @Test
    fun `登录成功时写入token与userId`() = runTest {
        val response = client(
            """{"status":"PASS","message":"登陆成功","token":"new-token","user_id":"u1"}""",
        ).login("2023001", "password")

        assertEquals("PASS", response.status)
        assertEquals("new-token", store.getToken())
        assertEquals("u1", store.getUserId())
    }

    @Test
    fun `登录成功但缺少token时不动旧会话`() = runTest {
        store.setToken("old-token")
        store.setUserId("old-user")

        val response = client(
            """{"status":"PASS","message":"登陆成功","user_id":"u1"}""",
        ).login("2023001", "password")

        assertEquals("PASS", response.status)
        assertNull(response.token)
        assertEquals("old-token", store.getToken())
        assertEquals("old-user", store.getUserId())
    }

    @Test
    fun `登录成功但缺少userId时不动旧会话`() = runTest {
        store.setToken("old-token")
        store.setUserId("old-user")

        val response = client(
            """{"status":"PASS","message":"登陆成功","token":"new-token"}""",
        ).login("2023001", "password")

        assertEquals("PASS", response.status)
        assertNull(response.userId)
        assertEquals("old-token", store.getToken())
        assertEquals("old-user", store.getUserId())
    }

    @Test
    fun `登录失败时不写入会话`() = runTest {
        val response = client(
            """{"status":"FAIL","message":"密码错误"}""",
        ).login("2023001", "password")

        assertEquals("FAIL", response.status)
        assertNull(store.getToken())
        assertNull(store.getUserId())
    }
}
