package restarhalf.stellar.schedule.data.remote

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 从 HTTP 响应体中提取服务器返回的错误消息。
 *
 * 尝试按优先级从 JSON 中读取常见错误字段（msg / Msg / message），
 * 并截断过长内容以避免展示大段技术文本。
 *
 * @param json 用于解析响应体的 Json 实例
 * @param fieldPriority 要尝试的 JSON 字段名列表，按优先级排列
 * @return 提取到的错误消息，解析失败或消息为空时返回 null
 */
internal suspend fun HttpResponse.extractServerErrorMessage(
    json: Json,
    fieldPriority: List<String> = listOf("msg", "Msg", "message"),
): String? {
    return try {
        val body = bodyAsText()
        if (body.isBlank()) return null
        val jsonObj = json.parseToJsonElement(body).jsonObject
        val msg = fieldPriority.firstNotNullOfOrNull { field ->
            jsonObj[field]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        }
        msg?.takeIf { it.length <= 50 }
    } catch (_: Exception) {
        null
    }
}
