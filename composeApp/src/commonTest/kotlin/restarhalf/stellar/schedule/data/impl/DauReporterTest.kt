package restarhalf.stellar.schedule.data.impl

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
import restarhalf.stellar.schedule.data.impl.settings.DeviceSettingsPortImpl
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

    private fun deviceSettings(vararg pairs: Pair<String, Any>) =
        DeviceSettingsPortImpl(MapSettings(*pairs))

    @Test
    fun privacyNotConfirmedNeverPings() = runTest {
        val settings = MapSettings()
        assertFalse(
            DauReporter.pingTodayIfDue(
                deviceSettings = DeviceSettingsPortImpl(settings),
                client = mockClient(HttpStatusCode.OK),
            ),
        )
        assertTrue(capturedUrls.isEmpty())
        assertNull(settings.getStringOrNull(SettingsKeys.DAU_LAST_PING_DAY))
    }

    @Test
    fun pingsOncePerDay() = runTest {
        val settings = MapSettings(SettingsKeys.CONFIRM_PRIVACY to true)
        val deviceSettings = DeviceSettingsPortImpl(settings)
        assertTrue(
            DauReporter.pingTodayIfDue(
                deviceSettings = deviceSettings,
                client = mockClient(HttpStatusCode.OK),
            ),
        )

        assertFalse(
            DauReporter.pingTodayIfDue(
                deviceSettings = deviceSettings,
                client = mockClient(HttpStatusCode.OK),
            ),
        )
        assertEquals(1, capturedUrls.size)

        assertFalse(settings.getStringOrNull(SettingsKeys.DAU_LAST_PING_DAY).isNullOrEmpty())
    }

    @Test
    fun requestTargetsVersionWorkerPing() = runTest {
        val client = mockClient(HttpStatusCode.OK)
        val deviceSettings = deviceSettings(
            SettingsKeys.CONFIRM_PRIVACY to true,
            SettingsKeys.DEVICE_ID to "aid-42",
        )
        assertTrue(DauReporter.pingTodayIfDue(deviceSettings = deviceSettings, client = client))
        assertEquals("$VERSION_WORKER_URL/ping", capturedUrls.single())
    }

    @Test
    fun httpFailureThrowsIllegalState() = runTest {
        val settings = MapSettings(SettingsKeys.CONFIRM_PRIVACY to true)
        val error = assertFailsWith<IllegalStateException> {
            DauReporter.pingTodayIfDue(
                deviceSettings = DeviceSettingsPortImpl(settings),
                client = mockClient(HttpStatusCode.BadGateway),
            )
        }
        assertTrue(error.message!!.contains("502"))

        assertNull(settings.getStringOrNull(SettingsKeys.DAU_LAST_PING_DAY))
    }
}
