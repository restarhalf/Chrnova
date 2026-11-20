package restarhalf.stellar.schedule.domain.model

object SettingsKeys {
    
    const val PREFS_NAME = "schedule_settings"
    
    const val SHOW_NON_CURRENT_WEEK = "show_non_current_week"
    
    const val SCHEDULE_ROW_HEIGHT = "schedule_row_height"
    
    const val REMINDER_ENABLED = "calendar_enabled"

    /** 课前提醒提前分钟数 */
    const val REMINDER_LEAD_MINUTES = "reminder_lead_minutes"

    /** 次日课程提醒开关 */
    const val NEXT_DAY_REMINDER_ENABLED = "next_day_reminder_enabled"

    /** 次日提醒时间（当天 0 点起的分钟数） */
    const val NEXT_DAY_REMINDER_MINUTE_OF_DAY = "next_day_reminder_minute_of_day"

    /** 课中提醒开关 */
    const val IN_CLASS_REMINDER_ENABLED = "in_class_reminder_enabled"

    /** 课中提醒时机：0=全程，1=距下课 N 分钟 */
    const val IN_CLASS_TIMING_MODE = "in_class_timing_mode"

    /** 距下课分钟数（0..60） */
    const val IN_CLASS_LEAD_MINUTES = "in_class_lead_minutes"

    /** 小米超级岛/焦点通知开关 */
    const val ISLAND_NOTIFICATION_ENABLED = "island_notification_enabled"

    /** 超级岛左侧：0=课程名称，1=上课地点，2=倒计时 */
    const val ISLAND_LEFT_MODE = "island_left_mode"

    /** 超级岛右侧：0=课程名称，1=上课地点，2=倒计时 */
    const val ISLAND_RIGHT_MODE = "island_right_mode"

    /** 超级岛息屏显示：0=课程名称，1=上课地点 */
    const val ISLAND_AOD_MODE = "island_aod_mode"

    /** 超级岛课中右区：0=正在上课，1=距下课倒计时 */
    const val ISLAND_IN_CLASS_RIGHT_MODE = "island_in_class_right_mode"

    /** 原生实况倒计时缩略态右区：0=课程名称，1=上课地点，2=倒计时 */
    const val LIVE_RIGHT_MODE = "live_right_mode"

    /** 上课自动勿扰开关 */
    const val CLASS_DND_ENABLED = "class_dnd_enabled"

    /** 勿扰档位：0=完全勿扰，1=静音，2=优先 */
    const val CLASS_DND_MODE = "class_dnd_mode"

    /** 勿扰接管会话：是否由本应用接管、接管档位 */
    const val CLASS_DND_APPLIED = "class_dnd_applied"
    const val CLASS_DND_APPLIED_MODE = "class_dnd_applied_mode"

    /** 接管前的系统原始值（铃声与勿扰两条通道都记） */
    const val CLASS_DND_ORIGINAL_RINGER = "class_dnd_original_ringer"
    const val CLASS_DND_ORIGINAL_FILTER = "class_dnd_original_filter"

    /** 接管后实测到的系统值，用于判断用户课中是否手动改过 */
    const val CLASS_DND_SET_RINGER = "class_dnd_set_ringer"
    const val CLASS_DND_SET_FILTER = "class_dnd_set_filter"

    /** 还原失败（多半权限被回收）后等待重试的起始时间 */
    const val CLASS_DND_DEFER_AT = "class_dnd_defer_at"

    /** 用户从通知/超级岛手动开启的勿扰：保持到再点一次、跨日或超时 */
    const val CLASS_DND_MANUAL = "dnd_manual_by_user"
    const val CLASS_DND_MANUAL_AT = "dnd_manual_at"

    /** 考试中提醒开关（照课中提醒） */
    const val IN_EXAM_REMINDER_ENABLED = "in_exam_reminder_enabled"

    /** 考试中提醒时机：0=全程，1=距结束 N 分钟 */
    const val IN_EXAM_TIMING_MODE = "in_exam_timing_mode"

    /** 距结束分钟数（0..60） */
    const val IN_EXAM_LEAD_MINUTES = "in_exam_lead_minutes"

    /** 考试自动勿扰开关 */
    const val EXAM_DND_ENABLED = "exam_dnd_enabled"

    /** 考试勿扰档位：0=完全勿扰，1=静音，2=优先 */
    const val EXAM_DND_MODE = "exam_dnd_mode"

    /** 考试勿扰接管会话：是否由本应用接管、接管档位 */
    const val EXAM_DND_APPLIED = "exam_dnd_applied"
    const val EXAM_DND_APPLIED_MODE = "exam_dnd_applied_mode"

    /** 接管前的系统原始值（铃声与勿扰两条通道都记） */
    const val EXAM_DND_ORIGINAL_RINGER = "exam_dnd_original_ringer"
    const val EXAM_DND_ORIGINAL_FILTER = "exam_dnd_original_filter"

    /** 接管后实测到的系统值，用于判断用户考中是否手动改过 */
    const val EXAM_DND_SET_RINGER = "exam_dnd_set_ringer"
    const val EXAM_DND_SET_FILTER = "exam_dnd_set_filter"

    /** 还原失败（多半权限被回收）后等待重试的起始时间 */
    const val EXAM_DND_DEFER_AT = "exam_dnd_defer_at"

    /** 用户从通知/超级岛手动开启的考试勿扰：保持到再点一次、跨日或超时 */
    const val EXAM_DND_MANUAL = "exam_dnd_manual_by_user"
    const val EXAM_DND_MANUAL_AT = "exam_dnd_manual_at"

    const val EXACT_ALARM_PERMISSION_ASKED = "exact_alarm_permission_asked"

    const val EXACT_ALARM_ALLOWED = "exact_alarm_allowed"
    
    const val THEME_MODE = "theme_mode"

    const val MATERIAL_LEVEL = "material_level"

    /** 小组件上下留白档位：0=标准 1=4×6 2=4×7 3=4×6 无字 4=4×7 无字 */
    const val WIDGET_PADDING_MODE = "widget_padding_mode"

    const val SELECTED_TERM = "selected_term"
    
    const val ACTIVE_SCHEDULE_TERM = "active_schedule_term"
    
    const val LAST_AUTO_SYNC_MS = "last_auto_sync_ms"
    
    const val CONFIRM_PRIVACY = "confirm_privacy"
    
    const val BACKGROUND_IMAGE_URI = "background_image_uri"
    
    const val BACKGROUND_ALPHA = "background_alpha"
    
    const val BACKGROUND_BLUR = "background_blur"

    const val BACKGROUND_BRIGHTNESS = "background_brightness"

    const val WALLPAPER_OFFSET_X = "wallpaper_offset_x"

    const val WALLPAPER_OFFSET_Y = "wallpaper_offset_y"

    const val WALLPAPER_SCALE = "wallpaper_scale"

    const val WALLPAPER_IS_DARK = "wallpaper_is_dark"

    const val COMPONENTS_ALPHA = "components_alpha"

    const val CARD_BLUR_PX = "card_blur_px"

    const val CARD_REFRACTION_LEVEL = "card_refraction_level"

    const val CARD_EDGE_THICKNESS = "card_edge_thickness"

    const val CARD_RIM_ENABLED = "card_rim_enabled"

    const val DEVICE_ID = "device_id"
    
    const val DAU_LAST_PING_DAY = "dau_last_ping_day"
    
    const val STAR_VERIFIED = "star_verified"

    const val CACHED_SEMESTER_IDS = "cached_semester_ids"

    const val CURRENT_TERM_ID = "current_term_id"

    const val USER_AVATAR_URI = "user_avatar_uri"
    
    const val USER_NICKNAME = "user_nickname"
}
