package restarhalf.stellar.schedule.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.domain.model.EvaluationCreateRequest
import restarhalf.stellar.schedule.domain.model.EvaluationPage
import restarhalf.stellar.schedule.domain.model.LikeResult
import restarhalf.stellar.schedule.domain.port.CourseEvaluationPort
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 课程评价 API 实现（Cloudflare Worker REST 接口）
 *
 * 镜像 PapersApi 的写法：持有 Ktor HttpClient，拼接 URL，手动编解码 JSON。
 */
class CourseEvaluationApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val getDeviceId: () -> String,
) : CourseEvaluationPort {

    private fun deviceHeader(request: HttpRequestBuilder) {
        val id = getDeviceId()
        if (id.isNotBlank()) request.header("X-Device-Id", id)
    }

    override suspend fun listEvaluations(
        course: String?,
        page: Int,
        size: Int,
    ): EvaluationPage = withContext(AppIoDispatcher) {
        val url = URLBuilder("$baseUrl/evaluations").apply {
            parameters.append("page", page.toString())
            parameters.append("size", size.toString())
            if (!course.isNullOrBlank()) parameters.append("course", course)
        }.buildString()
        val body = executeGet(url)
        json.decodeFromString(EvaluationPage.serializer(), body)
    }

    override suspend fun getEvaluation(id: String): Evaluation = withContext(AppIoDispatcher) {
        val body = executeGet("$baseUrl/evaluations/$id")
        json.decodeFromString(Evaluation.serializer(), body)
    }

    override suspend fun createEvaluation(req: EvaluationCreateRequest): Evaluation =
        withContext(AppIoDispatcher) {
            val response: HttpResponse = httpClient.post("$baseUrl/evaluations") {
                deviceHeader(this)
                contentType(ContentType.Application.Json)
                setBody(req)
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("提交评价失败（HTTP ${response.status.value}）")
            }
            json.decodeFromString(Evaluation.serializer(), response.body())
        }

    override suspend fun deleteEvaluation(id: String): Boolean = withContext(AppIoDispatcher) {
        val response: HttpResponse = httpClient.delete("$baseUrl/evaluations/$id") {
            deviceHeader(this)
        }
        response.status.isSuccess()
    }

    override suspend fun toggleLike(id: String): LikeResult = withContext(AppIoDispatcher) {
        val response: HttpResponse = httpClient.post("$baseUrl/evaluations/$id/like") {
            deviceHeader(this)
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("操作失败（HTTP ${response.status.value}）")
        }
        json.decodeFromString(LikeResult.serializer(), response.body())
    }

    private suspend fun executeGet(url: String): String {
        val response: HttpResponse = httpClient.get(url) { deviceHeader(this) }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("请求失败（HTTP ${response.status.value}）")
        }
        return response.body()
    }
}
