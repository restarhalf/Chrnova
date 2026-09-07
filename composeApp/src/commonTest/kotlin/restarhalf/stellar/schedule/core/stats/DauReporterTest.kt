package restarhalf.stellar.schedule.core.stats

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.core.update.VERSION_WORKER_URL
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DauReporterTest {

    private val capturedUrls = mutableListOf<String>()

    private fun mockClient(status: HttpStatusCode) = HttpClient(MockEngine) {
        install(ContentNegotiation) { json() }
        engine {
            addHandler { request ->
                capturedUrls.add(request.url.toString())
                respond(
                    content = """{"ok":true}""",
                    status = status,
                    headers = headers { append(HttpHeaders.ContentType, "application/json") },
                )
            }
        }
    }

    @Test
    fun privacyNotConfirmedNeverPings() = runTest {
        val settings = MapSettings()
        assertFalse(DauReporter.pingTodayIfDue(settings, "device-1", mockClient(HttpStatusCode.OK)))
        assertTrue(capturedUrls.isEmpty())
        assertNull(settings.getStringOrNull(SettingsKeys.DAU_LAST_PING_DAY))
    }

    @Test
    fun pingsOncePerDay() = runTest {
        val settings = MapSettings(SettingsKeys.CONFIRM_PRIVACY to true)
        assertTrue(DauReporter.pingTodayIfDue(settings, "device-1", mockClient(HttpStatusCode.OK)))
        // 当日第二次不再发送
        assertFalse(DauReporter.pingTodayIfDue(settings, "device-1", mockClient(HttpStatusCode.OK)))
        assertEquals(1, capturedUrls.size)
        // 已记录上报日期
        assertFalse(settings.getStringOrNull(SettingsKeys.DAU_LAST_PING_DAY).isNullOrEmpty())
    }

    @Test
    fun requestTargetsVersionWorkerPing() = runTest {
        val client = mockClient(HttpStatusCode.OK)
        val settings = MapSettings(SettingsKeys.CONFIRM_PRIVACY to true)
        assertTrue(DauReporter.pingTodayIfDue(settings, "aid-42", client))
        assertEquals("$VERSION_WORKER_URL/ping", capturedUrls.single())
    }

    @Test
    fun httpFailureThrowsIllegalState() = runTest {
        val settings = MapSettings(SettingsKeys.CONFIRM_PRIVACY to true)
        val error = assertFailsWith<IllegalStateException> {
            DauReporter.pingTodayIfDue(settings, "device-1", mockClient(HttpStatusCode.BadGateway))
        }
        assertTrue(error.message!!.contains("502"))
        // 失败后不记录日期，允许下次重试
        assertNull(settings.getStringOrNull(SettingsKeys.DAU_LAST_PING_DAY))
    }
}
