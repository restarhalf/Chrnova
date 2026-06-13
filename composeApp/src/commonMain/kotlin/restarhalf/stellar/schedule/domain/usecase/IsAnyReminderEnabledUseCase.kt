package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.port.SettingsPort

/**
 * 检查是否有任何提醒启用用例
 * 
 * 检查课程提醒或考试提醒是否至少有一个启用。
 */
class IsAnyReminderEnabledUseCase(
    private val settings: SettingsPort,
) {
    /**
     * 检查是否有任何提醒启用
     * 
     * @return 如果至少有一个提醒启用返回true
     */
    suspend operator fun invoke(): Boolean {
        val courseEnabled = settings.observeCourseReminderEnabled().first()
        val examEnabled = settings.observeExamReminderEnabled().first()
        return courseEnabled || examEnabled
    }
}
