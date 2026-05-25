package restarhalf.stellar.schedule.mcp

import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import restarhalf.stellar.schedule.agent.control.ClientCommandExecutor
import restarhalf.stellar.schedule.domain.model.agent.ClientCommandDto

class McpRuntime(
    private val transport: McpTransport,
    private val toolsRegistry: ToolsRegistry,
    private val clientCommandExecutor: ClientCommandExecutor,
    private val contextProvider: RuntimeContextProvider,
    private val authTokenProvider: McpAuthTokenProvider,
    private val json: Json,
    private val runtimeId: String,
) {
    private var job: Job? = null

    fun initialize(scope: CoroutineScope) {
        if (job != null) return
        toolsRegistry.registerDefaultTools()
        job = scope.launch {
            try {
                while (true) {
                    try {
                        transport.connect(
                            onConnected = { outgoing ->
                                with(transport) {
                                    outgoing.sendJson(
                                        mcpInitializeRequest(
                                            requestId = RUNTIME_INIT_REQUEST_ID,
                                            runtimeId = runtimeId,
                                            authToken = authTokenProvider.token(),
                                            context = contextProvider.snapshot(),
                                        ),
                                    )
                                }
                            },
                            onMessage = { request, outgoing -> handleMessage(request, outgoing) },
                        )
                    } catch (exc: CancellationException) {
                        throw exc
                    } catch (exc: Exception) {
                        println("McpRuntime connect failed, retrying in 5s: ${exc::class.simpleName}: ${exc.message}")
                    }
                    delay(5000)
                }
            } finally {
                job = null
            }
        }
    }

    private suspend fun handleMessage(request: JsonObject, outgoing: kotlinx.coroutines.channels.SendChannel<Frame>) {
        val id = request["id"]
        if (request["method"] == null && id?.jsonPrimitive?.contentOrNull == RUNTIME_INIT_REQUEST_ID) {
            sendInitialized(outgoing)
            return
        }
        when (request["method"]?.jsonPrimitive?.contentOrNull) {
            "initialize" -> outgoing.sendJson(
                result(
                    id,
                    buildJsonObject {
                        put("runtime", "ChrnovaClient")
                        put("runtimeId", runtimeId)
                        put("authToken", authTokenProvider.token())
                        put("context", contextProvider.snapshot())
                    },
                ),
            ).also {
                sendInitialized(outgoing)
            }
            "tools/list" -> outgoing.sendJson(
                result(
                    id,
                    buildJsonObject {
                        put("tools", json.encodeToJsonElement(ListSerializer(McpTool.serializer()), toolsRegistry.listTools()))
                    },
                ),
            )
            "tools/call" -> outgoing.sendJson(result(id, callTool(request["params"]?.jsonObject ?: JsonObject(emptyMap()))))
            "context/snapshot" -> outgoing.sendJson(result(id, contextProvider.snapshot()))
        }
    }

    suspend fun emitClientEvent(type: ClientEventType, payload: JsonObject = JsonObject(emptyMap())) {
        transport.sendClientEvent(type.wireName, payload)
    }

    suspend fun emitClientEvent(type: String, payload: JsonObject, outgoing: kotlinx.coroutines.channels.SendChannel<Frame>) {
        with(transport) {
            outgoing.sendNotification(
                "events/client",
                buildJsonObject {
                    put("runtimeId", runtimeId)
                    put("type", type)
                    put("payload", payload)
                },
            )
        }
    }

    private suspend fun callTool(params: JsonObject): JsonObject {
        val name = params["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val commandType = toolsRegistry.commandTypeFor(name)
            ?: return buildJsonObject {
                putMcpContent("Unknown MCP tool: $name")
                put("isError", true)
            }
        val arguments = params["arguments"]?.jsonObject.orEmpty().mapValues { (_, value) ->
            value.jsonPrimitive.contentOrNull.orEmpty()
        }
        val result = clientCommandExecutor.execute(
            ClientCommandDto(
                id = params["callId"]?.jsonPrimitive?.contentOrNull ?: name,
                conversationId = params["conversationId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                type = commandType,
                arguments = arguments,
                createdAt = "",
            ),
        )
        return buildJsonObject {
            putMcpContent(result.payload ?: result.error ?: "")
            put("isError", !result.success)
        }
    }

    private suspend fun sendInitialized(outgoing: kotlinx.coroutines.channels.SendChannel<Frame>) {
        with(transport) {
            outgoing.sendNotification("initialized", buildJsonObject { put("runtimeId", runtimeId) })
        }
    }

    private suspend fun kotlinx.coroutines.channels.SendChannel<Frame>.sendJson(payload: JsonObject) {
        with(transport) { sendJson(payload) }
    }

    private fun result(id: JsonElement?, payload: JsonObject): JsonObject = buildJsonObject {
        put("jsonrpc", "2.0")
        id?.let { put("id", it) }
        put("result", payload)
    }

    private fun JsonObjectBuilder.putMcpContent(text: String) {
        putJsonArray("content") {
            add(
                buildJsonObject {
                    put("type", "text")
                    put("text", text)
                },
            )
        }
    }

    enum class ClientEventType(val wireName: String) {
        TimetableUpdated("timetable_updated"),
        LoginExpired("login_expired"),
        SyncCompleted("sync_completed"),
        NetworkChanged("network_changed"),
        CapabilityChanged("capability_changed"),
    }
}

private const val RUNTIME_INIT_REQUEST_ID = "runtime-init"

fun mcpInitializeRequest(
    requestId: String,
    runtimeId: String,
    authToken: String,
    context: JsonObject,
): JsonObject = buildJsonObject {
    put("jsonrpc", "2.0")
    put("id", requestId)
    put("method", "initialize")
    putJsonObject("params") {
        put("runtimeId", runtimeId)
        put("authToken", authToken)
        put("context", context)
    }
}
