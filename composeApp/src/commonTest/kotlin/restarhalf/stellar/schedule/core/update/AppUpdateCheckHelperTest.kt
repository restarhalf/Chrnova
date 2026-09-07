package restarhalf.stellar.schedule.core.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdateCheckHelperTest {

    private fun mockClient(status: HttpStatusCode, payload: String, captured: MutableList<String>) =
        HttpClient(MockEngine) {
            install(ContentNegotiation) { json() }
            engine {
                addHandler { request ->
                    captured.add(request.url.toString())
                    respond(
                        content = payload,
                        status = status,
                        headers = headers { append(HttpHeaders.ContentType, "application/json") },
                    )
                }
            }
        }

    @Test
    fun newerVersionReturnsInfo() = runTest {
        val captured = mutableListOf<String>()
        val client = mockClient(
            HttpStatusCode.OK,
            """{"version":"9.9.9","url":"https://example.com/app.apk","changelog":"修复若干问题"}""",
            captured,
        )
        val info = checkUpdateFromWorker(currentVersionName = "1.0.0", client = client)
        assertEquals("9.9.9", info?.latestVersion)
        assertEquals("https://example.com/app.apk", info?.downloadUrl)
        assertEquals("https://example.com/app.apk", info?.releasePageUrl)
        assertEquals("修复若干问题", info?.changelog)
        assertEquals("$VERSION_WORKER_URL/version.json", captured.single())
    }

    @Test
    fun sameVersionReturnsNull() = runTest {
        val client = mockClient(HttpStatusCode.OK, """{"version":"1.0.0"}""", mutableListOf())
        assertNull(checkUpdateFromWorker(currentVersionName = "1.0.0", client = client))
    }

    @Test
    fun olderVersionReturnsNull() = runTest {
        val client = mockClient(HttpStatusCode.OK, """{"version":"0.9.0"}""", mutableListOf())
        assertNull(checkUpdateFromWorker(currentVersionName = "1.0.0", client = client))
    }

    @Test
    fun blankVersionReturnsNull() = runTest {
        val client = mockClient(HttpStatusCode.OK, """{"version":"   "}""", mutableListOf())
        assertNull(checkUpdateFromWorker(currentVersionName = "1.0.0", client = client))
    }

    @Test
    fun unknownFieldsIgnored() = runTest {
        val client = mockClient(
            HttpStatusCode.OK,
            """{"version":"2.0.0","extra":"whatever","gray":"x"}""",
            mutableListOf(),
        )
        val info = checkUpdateFromWorker(currentVersionName = "1.0.0", client = client)
        assertEquals("2.0.0", info?.latestVersion)
    }

    @Test
    fun blankUrlFallsBackToDefaultShareUrl() = runTest {
        val client = mockClient(HttpStatusCode.OK, """{"version":"2.0.0","url":""}""", mutableListOf())
        val info = checkUpdateFromWorker(currentVersionName = "1.0.0", client = client)
        assertEquals(DEFAULT_QUARK_SHARE_URL, info?.downloadUrl)
    }

    @Test
    fun httpErrorThrows() = runTest {
        val client = mockClient(HttpStatusCode.InternalServerError, "oops", mutableListOf())
        val error = assertFailsWith<IllegalStateException> {
            checkUpdateFromWorker(currentVersionName = "1.0.0", client = client)
        }
        assertTrue(error.message!!.contains("500"))
    }

    @Test
    fun grayUidAppendedToRequest() = runTest {
        val captured = mutableListOf<String>()
        val client = mockClient(HttpStatusCode.OK, """{"version":"1.0.0"}""", captured)
        checkUpdateFromWorker(currentVersionName = "1.0.0", grayUid = "abc123", client = client)
        assertEquals("$VERSION_WORKER_URL/version.json?uid=abc123", captured.single())
    }
}
