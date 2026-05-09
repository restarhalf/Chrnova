package restarhalf.stellar.schedule.data.remote.agent

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.model.agent.AgentConversationDto
import restarhalf.stellar.schedule.domain.model.agent.AgentMessageDto
import restarhalf.stellar.schedule.domain.model.agent.AgentStreamEventDto
import restarhalf.stellar.schedule.domain.model.agent.AgentStreamRequest
import restarhalf.stellar.schedule.domain.model.agent.ClientCommandResultRequest
import restarhalf.stellar.schedule.domain.model.agent.CreateAgentConversationRequest
import restarhalf.stellar.schedule.domain.model.agent.RenameAgentConversationRequest
import restarhalf.stellar.schedule.domain.model.agent.RevertAgentConversationRequest
import restarhalf.stellar.schedule.domain.port.AgentPort

class AgentClient(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String,
) : AgentPort {
    private val apiBase = baseUrl.trimEnd('/') + "/api/agent"

    init {
        println("ChrnovaAgentClient apiBase=$apiBase")
    }

    override suspend fun listConversations(): List<AgentConversationDto> = request("listConversations") {
        httpClient.get("$apiBase/conversations").body()
    }

    override suspend fun createConversation(title: String?): AgentConversationDto =
        request("createConversation") {
            httpClient.post("$apiBase/conversations") {
                contentType(ContentType.Application.Json)
                setBody(CreateAgentConversationRequest(title))
            }.body()
        }

    override suspend fun listMessages(conversationId: String): List<AgentMessageDto> = request("listMessages") {
        httpClient.get("$apiBase/conversations/$conversationId/messages").body()
    }

    override suspend fun renameConversation(conversationId: String, title: String): AgentConversationDto =
        request("renameConversation") {
            httpClient.patch("$apiBase/conversations/$conversationId") {
                contentType(ContentType.Application.Json)
                setBody(RenameAgentConversationRequest(title))
            }.body()
        }

    override suspend fun deleteConversation(conversationId: String) {
        request("deleteConversation") {
            httpClient.delete("$apiBase/conversations/$conversationId")
            Unit
        }
    }

    override suspend fun revertConversation(conversationId: String, messageId: String): List<AgentMessageDto> =
        request("revertConversation") {
            httpClient.post("$apiBase/conversations/$conversationId/revert") {
                contentType(ContentType.Application.Json)
                setBody(RevertAgentConversationRequest(messageId))
            }.body()
        }

    override fun streamMessage(conversationId: String, message: String): Flow<AgentStreamEventDto> = flow {
        httpClient.sse(
            urlString = "$apiBase/conversations/$conversationId/stream",
            request = {
                method = io.ktor.http.HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(AgentStreamRequest(message))
            },
        ) {
            incoming.collect { event ->
                val data = event.data ?: return@collect
                emit(json.decodeFromString(AgentStreamEventDto.serializer(), data))
            }
        }
    }

    override suspend fun stopConversation(conversationId: String) {
        request("stopConversation") {
            httpClient.post("$apiBase/conversations/$conversationId/stop")
            Unit
        }
    }

    override suspend fun postClientCommandResult(request: ClientCommandResultRequest) {
        request("postClientCommandResult") {
            httpClient.post("$apiBase/client-events") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Unit
        }
    }

    private suspend fun <T> request(name: String, block: suspend () -> T): T {
        println("ChrnovaAgentClient start $name apiBase=$apiBase")
        return runCatching { block() }
            .onSuccess { println("ChrnovaAgentClient success $name") }
            .onFailure { throwable ->
                println("ChrnovaAgentClient failure $name ${throwable::class.simpleName}: ${throwable.message}")
            }
            .getOrThrow()
    }
}
