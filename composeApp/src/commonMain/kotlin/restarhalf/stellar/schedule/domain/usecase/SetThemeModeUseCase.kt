package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

/**
 * 设置主题模式用例
 *
 * @param settings 设置端口
 */
class SetThemeModeUseCase(
    private val settings: SettingsPort,
) {
    /**
     * 设置主题模式
     *
     * @param mode 主题模式值
     */
    operator fun invoke(mode: Int) {
        settings.setThemeMode(mode)
    }
}
