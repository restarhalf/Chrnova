package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.AdConfig
import restarhalf.stellar.schedule.domain.port.AnnouncementPort

/**
 * 获取广告位配置用例。
 *
 * 直接透传端口的 [AdConfig]（后端未配置时端口返回 null），无缓存策略——
 * 广告配置体积极小且不常变动，每次进入列表页随 ViewModel 初始化拉取一次即可。
 */
class FetchAdConfigUseCase(
    private val port: AnnouncementPort,
) {
    suspend operator fun invoke(): AdConfig? = port.getAdConfig()
}
