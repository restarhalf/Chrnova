package restarhalf.stellar.schedule.domain.model

/**
 * 设置项键名常量类
 * 
 * 集中管理所有应用设置的键名，用于SharedPreferences/Settings存储。
 * 避免在代码中硬编码字符串，提高可维护性。
 */
object SettingsKeys {
    /** 设置存储文件名 */
    const val PREFS_NAME = "schedule_settings"
    /** 是否显示非当前周课程 */
    const val SHOW_NON_CURRENT_WEEK = "show_non_current_week"
    /** 课程提醒是否启用 */
    const val REMINDER_ENABLED = "calendar_enabled"
    /** 考试提醒是否启用 */
    const val EXAM_REMINDER_ENABLED = "exam_calendar_enabled"
    /** 主题模式：0=跟随系统，1=浅色，2=深色 */
    const val THEME_MODE = "theme_mode"
    /** 当前选中的学期 */
    const val SELECTED_TERM = "selected_term"
    /** 当前激活的课程表学期 */
    const val ACTIVE_SCHEDULE_TERM = "active_schedule_term"
    /** 上次自动同步的时间戳（毫秒） */
    const val LAST_AUTO_SYNC_MS = "last_auto_sync_ms"
    /** 是否已确认隐私协议 */
    const val CONFIRM_PRIVACY = "confirm_privacy"
    /** 背景图片URI */
    const val BACKGROUND_IMAGE_URI = "background_image_uri"
    /** 背景透明度（0.0-1.0） */
    const val BACKGROUND_ALPHA = "background_alpha"
    /** 背景模糊度（0.0-1.0） */
    const val BACKGROUND_BLUR = "background_blur"
    /** 组件透明度（0.0-1.0），用于控制背景上组件的可见度 */
    const val COMPONENTS_ALPHA = "components_alpha"
    /** 浮动导航栏模式：0=标准，1=紧凑 */
    const val FLOATING_BAR = "floating_bar"
    /** 是否开启日志 */
    const val LOG_ENABLED = "log_enabled"
    /** 设备唯一标识符 */
    const val DEVICE_ID = "device_id"
    /** 上次匿名日活上报的日期（UTC，yyyy-MM-dd） */
    const val DAU_LAST_PING_DAY = "dau_last_ping_day"
    /** Star 验证状态 */
    const val STAR_VERIFIED = "star_verified"

    /** 缓存的学期ID列表（JSON字符串） */
    const val CACHED_SEMESTER_IDS = "cached_semester_ids"

    /** 当前学期ID缓存 */
    const val CURRENT_TERM_ID = "current_term_id"

    /** 用户自定义头像URI */
    const val USER_AVATAR_URI = "user_avatar_uri"
    /** 用户自定义昵称 */
    const val USER_NICKNAME = "user_nickname"
}
