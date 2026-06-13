package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.SettingsPort

/**
 * 设置端口实现类
 * 
 * 实现SettingsPort接口，负责应用设置的读写操作。
 * 使用ObservableSettings进行跨平台设置存储。
 * 
 * @param settings ObservableSettings实例
 */
@OptIn(ExperimentalSettingsApi::class)
class SettingsPortImpl(
    private val settings: ObservableSettings,
) : SettingsPort {

    /**
     * 观察是否显示非当前周课程
     * 
     * @return 是否显示的Flow
     */
    override fun observeShowNonCurrentWeek(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.SHOW_NON_CURRENT_WEEK, true)
    }

    /**
     * 设置是否显示非当前周课程
     * 
     * @param show 是否显示
     */
    override fun setShowNonCurrentWeek(show: Boolean) {
        settings[SettingsKeys.SHOW_NON_CURRENT_WEEK] = show
    }

    /**
     * 观察主题模式
     * 
     * @return 主题模式Flow（0=跟随系统，1=浅色，2=深色）
     */
    override fun observeThemeMode(): Flow<Int> {
        return settings.getIntFlow(SettingsKeys.THEME_MODE, 0)
    }

    /**
     * 设置主题模式
     * 
     * @param mode 主题模式
     */
    override fun setThemeMode(mode: Int) {
        settings[SettingsKeys.THEME_MODE] = mode.coerceIn(0, 2)
    }

    /**
     * 观察浮动导航栏模式
     * 
     * @return 浮动导航栏模式Flow
     */
    override fun observeFloatingBar(): Flow<Int> {
        return settings.getIntFlow(SettingsKeys.FLOATING_BAR, 0)
    }

    /**
     * 设置浮动导航栏模式
     * 
     * @param mode 模式（0=固定，1=悬浮，2=液态玻璃）
     */
    override fun setFloatingBar(mode: Int) {
        settings[SettingsKeys.FLOATING_BAR] = mode.coerceIn(0,2)
    }

    /**
     * 观察选中的学期
     * 
     * @return 学期Flow
     */
    override fun observeSelectedTerm(): Flow<String> {
        return settings.getStringFlow(SettingsKeys.SELECTED_TERM, "")
    }

    /**
     * 设置选中的学期
     * 
     * @param term 学期
     */
    override fun setSelectedTerm(term: String) {
        settings[SettingsKeys.SELECTED_TERM] = term
    }

    /**
     * 观察激活的课程表学期
     * 
     * @return 学期Flow
     */
    override fun observeActiveScheduleTerm(): Flow<String> {
        return settings.getStringFlow(SettingsKeys.ACTIVE_SCHEDULE_TERM, "")
    }

    /**
     * 设置激活的课程表学期
     * 
     * @param term 学期
     */
    override fun setActiveScheduleTerm(term: String) {
        settings[SettingsKeys.ACTIVE_SCHEDULE_TERM] = term
    }

    /**
     * 观察课程提醒开关
     * 
     * @return 是否启用的Flow
     */
    override fun observeCourseReminderEnabled(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.REMINDER_ENABLED, false)
    }

    /**
     * 设置课程提醒开关
     * 
     * @param enabled 是否启用
     */
    override fun setCourseReminderEnabled(enabled: Boolean) {
        settings[SettingsKeys.REMINDER_ENABLED] = enabled
    }

    /**
     * 观察考试提醒开关
     * 
     * @return 是否启用的Flow
     */
    override fun observeExamReminderEnabled(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.EXAM_REMINDER_ENABLED, false)
    }

    /**
     * 设置考试提醒开关
     * 
     * @param enabled 是否启用
     */
    override fun setExamReminderEnabled(enabled: Boolean) {
        settings[SettingsKeys.EXAM_REMINDER_ENABLED] = enabled
    }

    /**
     * 检查是否需要自动同步并标记
     * 
     * 如果距离上次同步超过24小时，返回true并更新同步时间。
     * 
     * @param nowMs 当前时间戳（毫秒）
     * @return 如果需要同步返回true
     */
    override suspend fun shouldAutoSyncAndMark(nowMs: Long): Boolean {
        val lastAutoSyncMs = settings.getLong(SettingsKeys.LAST_AUTO_SYNC_MS, 0L)
        val cooldownMs = 24L * 60L * 60L * 1000L
        return if (nowMs - lastAutoSyncMs >= cooldownMs) {
            settings[SettingsKeys.LAST_AUTO_SYNC_MS] = nowMs
            true
        } else {
            false
        }
    }
}
