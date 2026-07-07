package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Paper
import restarhalf.stellar.schedule.domain.usecase.DownloadPaperUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchPaperDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchPaperFoldersUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchPapersUseCase
import restarhalf.stellar.schedule.domain.usecase.UploadPaperUseCase
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase

class PapersViewModel(
    private val fetchPapers: FetchPapersUseCase,
    private val fetchPaperFolders: FetchPaperFoldersUseCase,
    private val fetchPaperDetail: FetchPaperDetailUseCase,
    private val downloadPaperUseCase: DownloadPaperUseCase,
    private val uploadPaperUseCase: UploadPaperUseCase,
    private val verifyGitHubStar: VerifyGitHubStarUseCase,
) : ViewModel() {

    data class PapersUiState(
        val loading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val allPapers: List<Paper> = emptyList(),
        val folders: List<String> = emptyList(),
        val searchQuery: String = "",
        val selectedPaper: Paper? = null,
        val uploading: Boolean = false,
        val downloadUrl: String? = null,
        val githubUsername: String = "",
        val isStarVerified: Boolean = false,
        val verifyingStar: Boolean = false,
        val showStarDialog: Boolean = false,
    ) {
        val papers: List<Paper>
            get() = allPapers.filter { paper ->
                searchQuery.isEmpty() || paper.title.contains(searchQuery, ignoreCase = true)
            }
    }

    private val _uiState = MutableStateFlow(PapersUiState())
    val uiState: StateFlow<PapersUiState> = _uiState

    private val loadMutex = Mutex()

    init {
        val verified = verifyGitHubStar.isStarVerified()
        if (verified) {
            _uiState.value = PapersUiState(isStarVerified = true)
        }
    }

    fun loadPapers() {
        viewModelScope.launch {
            loadMutex.withLock {
                _uiState.update { it.copy(loading = true, error = null) }
                runCatching {
                    fetchPapers()
                }.onSuccess { papers ->
                    _uiState.update { it.copy(allPapers = papers, loading = false) }
                }.onFailure { e ->
                    AppLogger.log("Papers", "加载课件列表失败", e)
                    _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
                }
            }
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            runCatching {
                fetchPaperFolders()
            }.onSuccess { folders ->
                _uiState.update { it.copy(folders = folders) }
            }.onFailure {
                AppLogger.log("Papers", "加载文件夹列表失败", it)
            }
        }
    }

    fun loadPaperDetail(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                fetchPaperDetail(id)
            }.onSuccess { paper ->
                _uiState.update { it.copy(selectedPaper = paper, loading = false) }
            }.onFailure { e ->
                AppLogger.log("Papers", "加载课件详情失败", e)
                _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    fun downloadPaper(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                downloadPaperUseCase(id)
            }.onSuccess { url ->
                _uiState.update { it.copy(loading = false, downloadUrl = "https://v4.gh-proxy.org/$url") }
            }.onFailure { e ->
                AppLogger.log("Papers", "下载课件失败", e)
                _uiState.update { it.copy(loading = false, error = e.message ?: "下载失败") }
            }
        }
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
                uploadPaperUseCase(
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

    fun onGitHubUsernameChange(username: String) {
        _uiState.update { it.copy(githubUsername = username) }
    }

    fun showStarDialog() {
        _uiState.update { it.copy(showStarDialog = true) }
    }

    fun verifyStar() {
        val username = _uiState.value.githubUsername.trim()
        if (username.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(verifyingStar = true, error = null) }
            runCatching {
                verifyGitHubStar(username)
            }.onSuccess { starred ->
                _uiState.update {
                    it.copy(
                        verifyingStar = false,
                        isStarVerified = starred,
                        showStarDialog = !starred,
                        error = if (!starred) "未检测到 star，请先 star 仓库后再试" else null,
                    )
                }
            }.onFailure { e ->
                AppLogger.log("Papers", "验证 star 失败", e)
                _uiState.update {
                    it.copy(verifyingStar = false, error = e.message ?: "验证失败")
                }
            }
        }
    }
}