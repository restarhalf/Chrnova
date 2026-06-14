package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * 设置端口接口
 * 
 * 定义应用设置的抽象接口，提供响应式的数据观察和更新能力。
 */
interface SettingsPort {
    /** 观察是否显示非当前周课程 */
    fun observeShowNonCurrentWeek(): Flow<Boolean>
    /** 设置是否显示非当前周课程 */
    fun setShowNonCurrentWeek(show: Boolean)

    /** 观察主题模式变化 */
    fun observeThemeMode(): Flow<Int>
    /** 设置主题模式（0=跟随系统，1=浅色，2=深色） */
    fun setThemeMode(mode: Int)

    /** 观察选中的学期变化 */
    fun observeSelectedTerm(): Flow<String>
    /** 设置选中的学期 */
    fun setSelectedTerm(term: String)

    /** 观察激活的课程表学期变化 */
    fun observeActiveScheduleTerm(): Flow<String>
    /** 设置激活的课程表学期 */
    fun setActiveScheduleTerm(term: String)

    /** 观察课程提醒开关状态 */
    fun observeCourseReminderEnabled(): Flow<Boolean>
    /** 设置课程提醒开关 */
    fun setCourseReminderEnabled(enabled: Boolean)

    /** 观察考试提醒开关状态 */
    fun observeExamReminderEnabled(): Flow<Boolean>
    /** 设置考试提醒开关 */
    fun setExamReminderEnabled(enabled: Boolean)

    /** 观察浮动导航栏模式变化 */
    fun observeFloatingBar(): Flow<Int>
    /** 设置浮动导航栏模式 */
    fun setFloatingBar(mode: Int)

    /** 观察日志开关状态 */
    fun observeLogEnabled(): Flow<Boolean>
    /** 设置日志开关 */
    fun setLogEnabled(enabled: Boolean)

    /**
     * 检查是否需要自动同步，并标记已同步时间
     * 
     * @param nowMs 当前时间戳（毫秒）
     * @return 如果距离上次同步超过24小时返回true，否则返回false
     */
    suspend fun shouldAutoSyncAndMark(nowMs: Long): Boolean
}
