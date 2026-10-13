package restarhalf.stellar.schedule.core.stats

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import restarhalf.stellar.schedule.core.update.VERSION_WORKER_URL
import restarhalf.stellar.schedule.core.update.updateHttpClient
import restarhalf.stellar.schedule.domain.model.SettingsKeys

@Serializable
private data class DauPingBody(val aid: String)


internal object DauReporter {

    
    suspend fun pingTodayIfDue(
        settings: ObservableSettings,
        deviceId: String,
        client: HttpClient = updateHttpClient,
    ): Boolean {
        if (!settings.getBoolean(SettingsKeys.CONFIRM_PRIVACY, false)) return false

        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()
        if (settings.getStringOrNull(SettingsKeys.DAU_LAST_PING_DAY) == today) return false

        val response = client.post("$VERSION_WORKER_URL/ping") {
            contentType(ContentType.Application.Json)
            setBody(DauPingBody(aid = deviceId))
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("日活上报失败（HTTP ${response.status.value}）")
        }

        settings[SettingsKeys.DAU_LAST_PING_DAY] = today
        return true
    }
}
