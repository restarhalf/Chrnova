package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementPort

/**
 * 获取单条公告详情用例。
 *
 * 用于 id 不在已加载的公开列表中的场景（如 status='ad' 的隐藏广告公告），
 * 此时无法从列表缓存命中，需直接走公开详情接口拉取。公开详情接口对
 * status='published' 与 status='ad' 均可见，草稿仍返回 404。
 */
class FetchAnnouncementUseCase(
    private val port: AnnouncementPort,
) {
    suspend operator fun invoke(id: String): Announcement = port.getAnnouncement(id)
}
