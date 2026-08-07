package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Announcement

/**
 * 公告端口接口
 *
 * 定义公告相关的抽象接口，包括列表查询与详情获取。
 * 后端地址固定为 chrnova.announcement.restarhalf.dpdns.org。
 * 公告面向全体用户，无需登录态或设备标识。
 */
interface AnnouncementPort {
    /**
     * 获取已发布公告列表（置顶优先，发布时间倒序）。
     *
     * @param limit 返回条数上限（1-100）
     * @return 公告列表
     */
    suspend fun listAnnouncements(limit: Int = 50): List<Announcement>

    /**
     * 获取已发布公告详情。
     *
     * @param id 公告唯一标识
     * @return 公告详情
     */
    suspend fun getAnnouncement(id: String): Announcement
}
