package restarhalf.stellar.schedule.domain.model.agent

import kotlinx.serialization.Serializable

@Serializable
data class AgentConversationDto(
    val id: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
enum class AgentMessageRoleDto {
    USER,
    ASSISTANT,
    TOOL,
    SYSTEM,
}

@Serializable
data class AgentMessageDto(
    val id: String,
    val conversationId: String,
    val role: AgentMessageRoleDto,
    val content: String,
    val createdAt: String,
)

@Serializable
data class CreateAgentConversationRequest(
    val title: String? = null,
)

@Serializable
data class RenameAgentConversationRequest(
    val title: String,
)

@Serializable
data class RevertAgentConversationRequest(
    val messageId: String,
)

@Serializable
data class AgentStreamRequest(
    val message: String,
)

@Serializable
data class AgentStreamEventDto(
    val type: String,
    val messageId: String? = null,
    val delta: String? = null,
    val content: String? = null,
    val error: String? = null,
    val command: ClientCommandDto? = null,
)

@Serializable
enum class ClientCommandTypeDto {
    GET_COURSES,
    GET_GRADES,
    GET_EXAMS,
    ADD_LAB_COURSE,
    TRANSFER_COURSE,
    SCHEDULE_REMINDER,
    CANCEL_REMINDER,
    NAVIGATE,
    RUN_SYNC,
    SET_THEME_MODE,
    SET_FLOATING_BAR,
    SET_SHOW_NON_CURRENT_WEEK,
    SET_COURSE_REMINDER_ENABLED,
    SET_EXAM_REMINDER_ENABLED,
    SET_CAMPUS,
    SET_TERM_START,
    SET_TOTAL_WEEKS,
    REMEMBER,
    RECALL_MEMORY,
}

@Serializable
data class ClientCommandDto(
    val id: String,
    val conversationId: String,
    val type: ClientCommandTypeDto,
    val arguments: Map<String, String> = emptyMap(),
    val createdAt: String,
)

@Serializable
data class ClientCommandResultRequest(
    val commandId: String,
    val success: Boolean,
    val payload: String? = null,
    val error: String? = null,
)


