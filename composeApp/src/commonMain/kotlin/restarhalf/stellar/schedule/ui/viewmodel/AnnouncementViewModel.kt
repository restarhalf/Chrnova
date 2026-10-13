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
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.AdConfig
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.usecase.FetchAdConfigUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementsUseCase
import restarhalf.stellar.schedule.domain.usecase.MarkAnnouncementsReadUseCase

class AnnouncementViewModel(
    private val fetchAnnouncements: FetchAnnouncementsUseCase,
    private val markAnnouncementsRead: MarkAnnouncementsReadUseCase,
    private val fetchAdConfig: FetchAdConfigUseCase,
    private val fetchAnnouncement: FetchAnnouncementUseCase,
) : ViewModel() {

    @Stable
    data class AnnouncementUiState(
        val loading: Boolean = false,
        val error: String? = null,
        
        val loaded: Boolean = false,
        val announcements: ImmutableList<Announcement> = persistentListOf(),
        
        val unreadCount: Int = 0,
        
        val lastReadAtMs: Long = 0L,
        
        val selectedAnnouncement: Announcement? = null,
        
        val adConfig: AdConfig? = null,
    )

    private val _uiState = MutableStateFlow(AnnouncementUiState())
    val uiState: StateFlow<AnnouncementUiState> = _uiState

    init {
        load()
        loadAdConfig()
    }

    
    private fun loadAdConfig() {
        viewModelScope.launch {
            runCatching { fetchAdConfig() }
                .onSuccess { ad -> _uiState.update { it.copy(adConfig = ad) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Announcement", "加载广告配置失败", e)
                }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {

            if (!forceRefresh && _uiState.value.loaded) return@launch
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                fetchAnnouncements(forceRefresh)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        loaded = true,
                        announcements = result.announcements.toPersistentList(),
                        unreadCount = result.unreadCount,
                        lastReadAtMs = result.lastReadAtMs,
                    )
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Announcement", "加载公告失败", e)
                _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    
    fun refresh() {
        load(forceRefresh = true)
        loadAdConfig()
    }

    
    fun markAnnouncementRead(announcement: Announcement) {
        val targetMs = announcement.lastChangeAtMs
        markAnnouncementsRead(announcement)
        _uiState.update { state ->
            val lastReadAtMs = maxOf(state.lastReadAtMs, targetMs)
            state.copy(
                lastReadAtMs = lastReadAtMs,
                unreadCount = state.announcements.count { it.lastChangeAtMs > lastReadAtMs },
            )
        }
    }

    
    fun selectAnnouncement(id: String) {
        val local = _uiState.value.announcements.firstOrNull { a -> a.id == id }
        if (local != null) {
            _uiState.update { it.copy(selectedAnnouncement = local) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, selectedAnnouncement = null) }
            runCatching { fetchAnnouncement(id) }
                .onSuccess { a ->
                    _uiState.update { it.copy(loading = false, selectedAnnouncement = a) }
                }.onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Announcement", "加载公告详情失败 id=$id", e)
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "加载失败",
                            selectedAnnouncement = null,
                        )
                    }
                }
        }
    }
}
