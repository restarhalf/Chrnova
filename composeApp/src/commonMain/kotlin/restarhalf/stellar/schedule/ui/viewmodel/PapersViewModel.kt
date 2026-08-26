package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Paper
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase

class PapersViewModel(
    private val papersPort: PapersPort,
    private val settings: SettingsPort,
    verifyGitHubStar: VerifyGitHubStarUseCase,
) : ViewModel() {

    val starVerification = StarVerificationHolder(
        verifyGitHubStar = verifyGitHubStar,
        settings = settings,
        scope = viewModelScope,
    )

    @Stable
    data class PapersUiState(
        val loading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val allPapers: ImmutableList<Paper> = persistentListOf(),
        val folders: ImmutableList<String> = persistentListOf(),
        val searchQuery: String = "",
        val selectedPaper: Paper? = null,
        val uploading: Boolean = false,
        val downloadUrl: String? = null,
    ) {
        val papers: List<Paper>
            get() = allPapers.filter { paper ->
                searchQuery.isEmpty() || paper.title.contains(searchQuery, ignoreCase = true)
            }
    }

    private val _uiState = MutableStateFlow(PapersUiState())
    val uiState: StateFlow<PapersUiState> = _uiState

    private val loadMutex = Mutex()

    fun loadPapers() {
        viewModelScope.launch {
            loadMutex.withLock {
                _uiState.update { it.copy(loading = true, error = null) }
                runCatching {
                    papersPort.listPapers()
                }.onSuccess { papers ->
                    _uiState.update { it.copy(allPapers = papers.toPersistentList(), loading = false) }
                }.onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Papers", "加载课件列表失败", e)
                    _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
                }
            }
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            runCatching {
                papersPort.getFolders()
            }.onSuccess { folders ->
                _uiState.update { it.copy(folders = folders.toPersistentList()) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Papers", "加载文件夹列表失败", e)
            }
        }
    }

    fun loadPaperDetail(id: String) {
        viewModelScope.launch {
            // 打开新详情时清掉上一次的下载链接，避免误触发旧试卷的下载
            _uiState.update { it.copy(loading = true, error = null, downloadUrl = null) }
            runCatching {
                papersPort.getPaper(id)
            }.onSuccess { paper ->
                _uiState.update { it.copy(selectedPaper = paper, loading = false) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Papers", "加载课件详情失败", e)
                _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    fun downloadPaper(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                papersPort.downloadPaper(id)
            }.onSuccess { url ->
                _uiState.update { it.copy(loading = false, downloadUrl = "https://v4.gh-proxy.org/$url") }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Papers", "下载课件失败", e)
                _uiState.update { it.copy(loading = false, error = e.message ?: "下载失败") }
            }
        }
    }

    /** UI 消费掉 [PapersUiState.downloadUrl] 后调用，置空防止重复触发 */
    fun consumeDownloadUrl() {
        _uiState.update { it.copy(downloadUrl = null) }
    }

    fun uploadPaper(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        title: String,
        folder: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploading = true, error = null) }
            runCatching {
                papersPort.uploadPaper(
                    fileBytes = fileBytes,
                    fileName = fileName,
                    mimeType = mimeType,
                    title = title,
                    folder = folder,
                )
            }.onSuccess {
                _uiState.update { it.copy(uploading = false, successMessage = "上传成功") }
                loadPapers()
                loadFolders()
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Papers", "上传课件失败", e)
                _uiState.update { it.copy(uploading = false, error = e.message ?: "上传失败") }
            }
        }
    }

    fun refresh() {
        loadFolders()
        loadPapers()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
