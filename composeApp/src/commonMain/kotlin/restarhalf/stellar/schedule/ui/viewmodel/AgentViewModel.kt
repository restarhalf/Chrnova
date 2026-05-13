package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.agent.control.ClientCommandExecutor
import restarhalf.stellar.schedule.domain.model.agent.AgentConversationDto
import restarhalf.stellar.schedule.domain.model.agent.AgentMessageDto
import restarhalf.stellar.schedule.domain.model.agent.AgentMessageRoleDto
import restarhalf.stellar.schedule.domain.port.AgentPort
import kotlin.random.Random

class AgentViewModel(
    private val agentPort: AgentPort,
    private val clientCommandExecutor: ClientCommandExecutor,
) : ViewModel() {
    data class Message(
        val id: String,
        val text: String,
        val fromUser: Boolean,
        val streaming: Boolean = false,
    )

    data class Conversation(
        val id: String,
        val summary: String,
    )

    data class AgentUiState(
        val streaming: Boolean,
        val userInput: String,
        val messages: List<Message>,
        val conversations: List<Conversation>,
        val activeConversationId: String?,
        val drawerOpen: Boolean,
        val errorMessage: String?,
    )

    private val _streaming = MutableStateFlow(false)
    private val _userInput = MutableStateFlow("")
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _activeConversationId = MutableStateFlow<String?>(null)
    private val _drawerOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private var streamJob: Job? = null

    private val baseUiState = combine(_streaming, _userInput, _messages, _conversations) { streaming, userInput, messages, conversations ->
        AgentUiState(
            streaming = streaming,
            userInput = userInput,
            messages = messages,
            conversations = conversations,
            activeConversationId = null,
            drawerOpen = false,
            errorMessage = null,
        )
    }

    private val _uiState: StateFlow<AgentUiState> = combine(baseUiState, _activeConversationId, _drawerOpen, _errorMessage) { base, activeConversationId, drawerOpen, errorMessage ->
        base.copy(
            activeConversationId = activeConversationId,
            drawerOpen = drawerOpen,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AgentUiState(
            streaming = false,
            userInput = "",
            messages = emptyList(),
            conversations = emptyList(),
            activeConversationId = null,
            drawerOpen = false,
            errorMessage = null,
        ),
    )

    val uiState: StateFlow<AgentUiState> = _uiState

    init {
        loadConversations()
    }

    fun sendMessage() {
        val text = _userInput.value.trim()
        if (text.isBlank() || _streaming.value) return
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            runCatching {
                _errorMessage.value = null
                val conversationId = ensureConversation().id
                val userMessage = Message(id = localId("user"), text = text, fromUser = true)
                val assistantMessage = Message(id = localId("assistant"), text = "", fromUser = false, streaming = true)
                _messages.update { it + userMessage + assistantMessage }
                _userInput.value = ""
                _streaming.value = true

                agentPort.streamMessage(conversationId, text).collect { event ->
                    when (event.type) {
                        "message_started" -> Unit
                        "delta" -> event.delta?.let { appendAssistantDelta(assistantMessage.id, it) }
                        "message_completed" -> completeAssistantMessage(assistantMessage.id, event.content)
                        "error" -> _errorMessage.value = event.error ?: "AI 响应失败"
                        "client_command" -> event.command?.let { command ->
                            val result = clientCommandExecutor.execute(command)
                            agentPort.postClientCommandResult(result)
                        }
                    }
                }
                completeAssistantMessage(assistantMessage.id, null)
                refreshMessages(conversationId)
                loadConversations()
            }.onFailure { throwable ->
                _errorMessage.value = throwable.message ?: "AI 服务连接失败"
                _messages.update { messages -> messages.map { if (it.streaming) it.copy(streaming = false) else it } }
            }
            _streaming.value = false
        }
    }

    fun stopMessage() {
        val conversationId = _activeConversationId.value
        streamJob?.cancel()
        streamJob = null
        _streaming.value = false
        _messages.update { messages -> messages.map { if (it.streaming) it.copy(streaming = false) else it } }
        if (conversationId != null) {
            viewModelScope.launch { runCatching { agentPort.stopConversation(conversationId) } }
        }
    }

    fun revertMessage(id: String) {
        val conversationId = _activeConversationId.value ?: return
        if (_streaming.value) stopMessage()
        val currentMessages = _messages.value
        val clickedIndex = currentMessages.indexOfFirst { it.id == id }
        if (clickedIndex == -1) return

        var userIndex = -1
        for (i in clickedIndex downTo 0) {
            if (currentMessages[i].fromUser) {
                userIndex = i
                break
            }
        }

        if (userIndex == -1) return

        val userMessage = currentMessages[userIndex]
        val targetId = if (userIndex > 0) currentMessages[userIndex - 1].id else ""

        viewModelScope.launch {
            runCatching { agentPort.revertConversation(conversationId, targetId) }
                .onSuccess {
                    _messages.value = it.map(::toUiMessage)
                    _userInput.value = userMessage.text
                }
                .onFailure { _errorMessage.value = it.message ?: "回溯失败" }
        }
    }


    fun loadConversations() {
        viewModelScope.launch {
            runCatching { agentPort.listConversations() }
                .onSuccess { conversations ->
                    _conversations.value = conversations.map(::toUiConversation)
                    if (_activeConversationId.value == null) conversations.firstOrNull()?.let { transConversation(it.id) }
                }
                .onFailure { _errorMessage.value = it.message ?: "加载对话失败" }
        }
    }

    fun newConversation() {
        viewModelScope.launch {
            runCatching { agentPort.createConversation("新对话") }
                .onSuccess { conversation ->
                    _activeConversationId.value = conversation.id
                    _messages.value = emptyList()
                    _drawerOpen.value = false
                    loadConversations()
                }
                .onFailure { _errorMessage.value = it.message ?: "创建对话失败" }
        }
    }

    fun transConversation(id: String) {
        viewModelScope.launch {
            runCatching { agentPort.listMessages(id) }
                .onSuccess { messages ->
                    _activeConversationId.value = id
                    _messages.value = messages.map(::toUiMessage)
                    _drawerOpen.value = false
                }
                .onFailure { _errorMessage.value = it.message ?: "切换对话失败" }
        }
    }

    fun renameConversation(id: String) {
        viewModelScope.launch {
            val title = _conversations.value.firstOrNull { it.id == id }?.summary.orEmpty()
            runCatching { agentPort.renameConversation(id, "$title*") }
                .onSuccess { loadConversations() }
                .onFailure { _errorMessage.value = it.message ?: "重命名失败" }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            runCatching { agentPort.deleteConversation(id) }
                .onSuccess {
                    if (_activeConversationId.value == id) {
                        _activeConversationId.value = null
                        _messages.value = emptyList()
                    }
                    loadConversations()
                }
                .onFailure { _errorMessage.value = it.message ?: "删除失败" }
        }
    }

    fun onUserInputChange(value: String) {
        _userInput.value = value
    }

    fun onDrawerOpenChange(open: Boolean) {
        _drawerOpen.value = open
    }

    private suspend fun ensureConversation(): AgentConversationDto {
        val activeId = _activeConversationId.value
        if (activeId != null) {
            val current = _conversations.value.firstOrNull { it.id == activeId }
            if (current != null) return AgentConversationDto(current.id, current.summary, "", "")
        }
        val created = agentPort.createConversation("新对话")
        _activeConversationId.value = created.id
        loadConversations()
        return created
    }

    private suspend fun refreshMessages(conversationId: String) {
        runCatching { agentPort.listMessages(conversationId) }
            .onSuccess { _messages.value = it.map(::toUiMessage) }
    }

    private fun appendAssistantDelta(messageId: String, delta: String) {
        _messages.update { messages ->
            messages.map { message -> if (message.id == messageId) message.copy(text = message.text + delta) else message }
        }
    }

    private fun completeAssistantMessage(messageId: String, content: String?) {
        _messages.update { messages ->
            messages.map { message ->
                if (message.id == messageId) message.copy(text = content ?: message.text, streaming = false) else message
            }
        }
    }

    private fun toUiConversation(conversation: AgentConversationDto): Conversation =
        Conversation(id = conversation.id, summary = conversation.title)

    private fun toUiMessage(message: AgentMessageDto): Message =
        Message(id = message.id, text = message.content, fromUser = message.role == AgentMessageRoleDto.USER)

    private fun localId(prefix: String): String = "$prefix-${Random.nextLong()}"
}
