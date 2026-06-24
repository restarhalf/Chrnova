package restarhalf.stellar.schedule.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import restarhalf.stellar.schedule.core.log.AppLogger
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import restarhalf.stellar.schedule.domain.model.Paper
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.platform.AppIoDispatcher

@Serializable
data class DownloadResponse(val url: String, val title: String = "")

class PapersApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val getDeviceId: () -> String,
    private val readFileBytes: suspend (String) -> ByteArray = { ByteArray(0) },
) : PapersPort {

    private fun deviceHeader(request: HttpRequestBuilder) {
        request.header("X-Device-Id", getDeviceId())
    }

    override suspend fun listPapers(): List<Paper> =
        withContext(AppIoDispatcher) {
            val body = executeGet("$baseUrl/papers")
            json.decodeFromString(ListSerializer(Paper.serializer()), body)
        }

    override suspend fun getCourses(): List<String> =
        withContext(AppIoDispatcher) {
            val body = executeGet("$baseUrl/courses")
            json.decodeFromString(ListSerializer(serializer<String>()), body)
        }

    override suspend fun getFolders(): List<String> =
        withContext(AppIoDispatcher) {
            val body = executeGet("$baseUrl/folders")
            json.decodeFromString(ListSerializer(serializer<String>()), body)
        }

    override suspend fun getPaper(id: String): Paper =
        withContext(AppIoDispatcher) {
            val body = executeGet("$baseUrl/papers/$id")
            json.decodeFromString(Paper.serializer(), body)
        }

    override suspend fun downloadPaper(id: String): String =
        withContext(AppIoDispatcher) {
            val response: HttpResponse = httpClient.get("$baseUrl/download/$id") {
                deviceHeader(this)
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("下载失败（HTTP ${response.status.value}）")
            }
            val body = json.decodeFromString<DownloadResponse>(response.bodyAsText())
            body.url
        }

    override suspend fun uploadPaper(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        title: String,
        folder: String,
    ): Paper =
        withContext(AppIoDispatcher) {
            val response: HttpResponse = httpClient.post("$baseUrl/upload") {
                deviceHeader(this)
                setBody(MultiPartFormDataContent(
                    formData {
                        append("title", title)
                        append("folder", folder)
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                ))
            }

            if (!response.status.isSuccess()) {
                throw IllegalStateException("上传失败（HTTP ${response.status.value}）")
            }

            json.decodeFromString(Paper.serializer(), response.body())
        }

    override suspend fun deletePaper(id: String): Boolean =
        withContext(AppIoDispatcher) {
            val response: HttpResponse = httpClient.delete("$baseUrl/papers/$id") {
                deviceHeader(this)
            }
            response.status.isSuccess()
        }

    @Serializable
    private data class VerifyStarResponse(val starred: Boolean, val username: String = "", val error: String? = null)

    override suspend fun verifyStar(username: String): Boolean =
        withContext(AppIoDispatcher) {
            val url = "$baseUrl/verify-star?username=$username"
            val body = executeGet(url)
            val result = json.decodeFromString<VerifyStarResponse>(body)
            if (result.error != null) {
                throw IllegalStateException(result.error)
            }
            result.starred
        }

    private suspend fun executeGet(url: String): String {
        val response: HttpResponse = httpClient.get(url) {
            deviceHeader(this)
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("请求失败（HTTP ${response.status.value}）")
        }
        return response.body()
    }
}
