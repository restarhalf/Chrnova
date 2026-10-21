package restarhalf.stellar.schedule.core.stats

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
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort

@Serializable
private data class DauPingBody(val aid: String)


internal object DauReporter {

    
    suspend fun pingTodayIfDue(
        deviceSettings: DeviceSettingsPort,
        client: HttpClient = updateHttpClient,
    ): Boolean {
        if (!deviceSettings.getConfirmPrivacy()) return false

        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()
        if (deviceSettings.getDauLastPingDay() == today) return false

        val response = client.post("$VERSION_WORKER_URL/ping") {
            contentType(ContentType.Application.Json)
            setBody(DauPingBody(aid = deviceSettings.getDeviceId()))
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("日活上报失败（HTTP ${response.status.value}）")
        }

        deviceSettings.setDauLastPingDay(today)
        return true
    }
}
