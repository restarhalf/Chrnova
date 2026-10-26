package restarhalf.stellar.schedule.data.remote

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
