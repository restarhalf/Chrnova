package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

/**
 * 检查是否需要自动同步用例
 * 
 * 检查距离上次同步是否超过24小时，如果是则标记需要同步。
 */
class ShouldAutoSyncAndMarkUseCase(
    private val settings: SettingsPort,
) {
    /**
     * 检查是否需要自动同步
     * 
     * @param nowMs 当前时间戳（毫秒）
     * @return 如果需要同步返回true
     */
    suspend operator fun invoke(nowMs: Long): Boolean = settings.shouldAutoSyncAndMark(nowMs)
}
