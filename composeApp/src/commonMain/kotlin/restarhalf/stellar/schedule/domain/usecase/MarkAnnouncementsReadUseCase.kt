package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.domain.model.Announcement

/**
 * 标记公告已读用例
 *
 * 把最后阅读时间推进到指定公告的内容最后变化时间（只前进、不后退），
 * 该条及内容变化时间早于它的公告均视为已读；新发布或被编辑的公告
 * （内容变化时间晚于记录时间）仍会重新出现未读红点。
 */
class MarkAnnouncementsReadUseCase(
    private val store: AnnouncementStore,
) {
    /**
     * 标记某条公告已读。
     *
     * @param announcement 用户点开查看的公告
     */
    operator fun invoke(announcement: Announcement) {
        val targetMs = announcement.lastChangeAtMs
        if (targetMs > store.getLastReadAtMs()) {
            store.setLastReadAtMs(targetMs)
        }
    }
}
