package restarhalf.stellar.schedule.mcp

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class McpTransport(
    private val httpClient: HttpClient,
    private val json: Json,
    baseUrl: String,
    private val runtimeId: String,
) {
    private val wsUrl = baseUrl.trimEnd('/').replaceFirst("http://", "ws://").replaceFirst("https://", "wss://") + "/mcp/ws"
    private val activeOutgoing = MutableStateFlow<SendChannel<Frame>?>(null)

    suspend fun connect(
        onConnected: suspend (SendChannel<Frame>) -> Unit,
        onMessage: suspend (JsonObject, SendChannel<Frame>) -> Unit,
    ) {
        httpClient.webSocket(wsUrl) {
            activeOutgoing.value = outgoing
            onConnected(outgoing)
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    onMessage(json.parseToJsonElement(frame.readText()).jsonObject, outgoing)
                }
            }
            activeOutgoing.value = null
        }
    }

    suspend fun sendClientEvent(type: String, payload: JsonObject) {
        activeOutgoing.value?.sendNotification(
            "events/client",
            buildJsonObject {
                put("runtimeId", runtimeId)
                put("type", type)
                put("payload", payload)
            },
        )
    }

    suspend fun SendChannel<Frame>.sendJson(payload: JsonObject) {
        send(Frame.Text(json.encodeToString(JsonObject.serializer(), payload)))
    }

    suspend fun SendChannel<Frame>.sendNotification(method: String, params: JsonObject) {
        sendJson(
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", method)
                put("params", params)
            },
        )
    }
}
