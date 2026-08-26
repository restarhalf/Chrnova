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
        /** 首次加载是否完成（未完成时首页卡片不展示，避免闪烁） */
        val loaded: Boolean = false,
        val announcements: ImmutableList<Announcement> = persistentListOf(),
        /** 未读公告数量（发布时间晚于最后阅读时间） */
        val unreadCount: Int = 0,
        /** 最后阅读时间（毫秒），列表页据此在每条公告右上角显示未读红点 */
        val lastReadAtMs: Long = 0L,
        /** 当前查看的公告详情 */
        val selectedAnnouncement: Announcement? = null,
        /** 公告列表页顶部广告位配置；后端未下发时为 null（广告位隐藏） */
        val adConfig: AdConfig? = null,
    )

    private val _uiState = MutableStateFlow(AnnouncementUiState())
    val uiState: StateFlow<AnnouncementUiState> = _uiState

    init {
        load()
        loadAdConfig()
    }

    /** 拉取广告位配置；失败不影响主流程，广告位保持隐藏（adConfig 默认 null） */
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
            // 非强制刷新时若已加载过（缓存命中），不重复打网络
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

    /** 下拉刷新 / App 回到前台：公告列表与广告位配置都重拉，保证后端改动实时生效 */
    fun refresh() {
        load(forceRefresh = true)
        loadAdConfig()
    }

    /** 把最后阅读时间推进到该条公告的内容最后变化时间（单调递增），并同步重算未读数量 */
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

    /** 选中详情：优先从已加载的公开列表命中（缓存）；命中不到（如 status='ad' 隐藏公告）则单独拉取 */
    fun selectAnnouncement(id: String) {
        val local = _uiState.value.announcements.firstOrNull { a -> a.id == id }
        if (local != null) {
            _uiState.update { it.copy(selectedAnnouncement = local) }
            return
        }
        // 不在列表缓存中：走公开详情接口单拉（后端对 ad 状态已放行）
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
