package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.agent.AgentConversationDto
import restarhalf.stellar.schedule.domain.model.agent.AgentMessageDto
import restarhalf.stellar.schedule.domain.model.agent.AgentStreamEventDto

interface AgentPort {
    suspend fun listConversations(): List<AgentConversationDto>
    suspend fun createConversation(title: String? = null): AgentConversationDto
    suspend fun listMessages(conversationId: String): List<AgentMessageDto>
    suspend fun renameConversation(conversationId: String, title: String): AgentConversationDto
    suspend fun deleteConversation(conversationId: String)
    suspend fun revertConversation(conversationId: String, messageId: String): List<AgentMessageDto>
    fun streamMessage(conversationId: String, message: String): Flow<AgentStreamEventDto>
    fun confirmToolCall(conversationId: String, toolCallId: String, approved: Boolean): Flow<AgentStreamEventDto>
    suspend fun stopConversation(conversationId: String)
}
