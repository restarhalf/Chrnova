package restarhalf.stellar.schedule.core.stats

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
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

/**
 * 匿名日活上报
 *
 * 复用本地随机生成的设备标识（不含学号等任何个人信息），在确认隐私政策后的每个
 * 自然日（UTC）首次启动时向版本 Worker 发送一次心跳；服务端按（日期, 设备标识）
 * 去重，仅用于统计每日活跃设备数。说明见隐私政策第一节。
 */
internal object DauReporter {

    /**
     * 今日尚未上报则发送一次心跳，成功后记录日期。
     *
     * @return true 表示本次实际发送成功，false 表示今日已上报或未确认隐私政策
     * @throws IllegalStateException 网络请求失败时抛出，由调用方决定是否重试
     */
    suspend fun pingTodayIfDue(settings: ObservableSettings, deviceId: String): Boolean {
        if (!settings.getBoolean(SettingsKeys.CONFIRM_PRIVACY, false)) return false

        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()
        if (settings.getStringOrNull(SettingsKeys.DAU_LAST_PING_DAY) == today) return false

        val response = updateHttpClient.post("$VERSION_WORKER_URL/ping") {
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
