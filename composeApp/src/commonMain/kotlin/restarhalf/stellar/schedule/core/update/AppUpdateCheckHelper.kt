package restarhalf.stellar.schedule.core.update

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val updateJson = Json { ignoreUnknownKeys = true }

internal val updateHttpClient = HttpClient {
    install(ContentNegotiation) { json(updateJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 10_000
    }
}

internal suspend fun checkUpdateFromWorker(currentVersionName: String): AppUpdateInfo? {
    val response = updateHttpClient.get(buildVersionWorkerUrl())
    if (!response.status.isSuccess()) {
        throw IllegalStateException("检查更新失败（HTTP ${response.status.value}）")
    }

    val payload: String = response.body()
    val versionInfo = updateJson.decodeFromString(VersionJsonResponse.serializer(), payload)
    val latestVersion = versionInfo.version.trim()
    if (latestVersion.isBlank() || !isNewerVersion(latestVersion, currentVersionName)) {
        return null
    }

    return AppUpdateInfo(
        latestVersion = latestVersion,
        releasePageUrl = QUARK_SHARE_URL,
        downloadUrl = QUARK_SHARE_URL,
        changelog = versionInfo.changelog,
    )
}
