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
