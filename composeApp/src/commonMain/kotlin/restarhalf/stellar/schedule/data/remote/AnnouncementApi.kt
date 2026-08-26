package restarhalf.stellar.schedule.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.model.AdConfig
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 公告 API 实现（Cloudflare Worker REST 接口）
 *
 * 镜像 PapersApi / CourseEvaluationApi 的写法：持有 Ktor HttpClient，拼接 URL，手动编解码 JSON。
 * 公告为全局数据，请求不携带登录态或设备标识。
 */
class AnnouncementApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AnnouncementPort {

    override suspend fun listAnnouncements(limit: Int): List<Announcement> =
        withContext(AppIoDispatcher) {
            val body = executeGet("$baseUrl/announcements?limit=$limit")
            json.decodeFromString(ListSerializer(Announcement.serializer()), body)
        }

    override suspend fun getAnnouncement(id: String): Announcement =
        withContext(AppIoDispatcher) {
            val body = executeGet("$baseUrl/announcements/$id")
            json.decodeFromString(Announcement.serializer(), body)
        }

    override suspend fun getAdConfig(): AdConfig? =
        withContext(AppIoDispatcher) {
            val response: HttpResponse = httpClient.get("$baseUrl/ad")
            if (!response.status.isSuccess()) return@withContext null
            val raw = response.body<String>()
            // 后端未配置/未启用时返回 JSON 字面量 null，需显式判定为空配置
            if (raw.isBlank() || raw.trim() == "null") return@withContext null
            json.decodeFromString<AdConfig>(raw)
        }

    private suspend fun executeGet(url: String): String {
        val response: HttpResponse = httpClient.get(url)
        if (!response.status.isSuccess()) {
            throw IllegalStateException("请求失败（HTTP ${response.status.value}）")
        }
        return response.body()
    }
}
