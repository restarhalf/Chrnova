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
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementsUseCase
import restarhalf.stellar.schedule.domain.usecase.MarkAnnouncementsReadUseCase

class AnnouncementViewModel(
    private val fetchAnnouncements: FetchAnnouncementsUseCase,
    private val markAnnouncementsRead: MarkAnnouncementsReadUseCase,
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
    )

    private val _uiState = MutableStateFlow(AnnouncementUiState())
    val uiState: StateFlow<AnnouncementUiState> = _uiState

    init {
        load()
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

    fun refresh() = load(forceRefresh = true)

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

    /** 从已加载的列表里按 id 取出详情（列表页保证详情数据已加载） */
    fun selectAnnouncement(id: String) {
        _uiState.update {
            it.copy(selectedAnnouncement = it.announcements.firstOrNull { a -> a.id == id })
        }
    }
}
