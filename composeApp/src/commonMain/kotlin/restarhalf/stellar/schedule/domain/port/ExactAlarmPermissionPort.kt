package restarhalf.stellar.schedule.domain.port

interface ExactAlarmPermissionPort {

    /** 系统当前是否允许排精确闹钟；没有这项权限的平台恒为 true。 */
    fun isExactAlarmAllowed(): Boolean

    /** 打开系统的精确闹钟授权页；已经允许或平台不支持时返回 false。 */
    fun openExactAlarmSettings(): Boolean

    /** 是否已经就精确闹钟权限弹过系统设置页；只弹一次，避免每次冷启动都跳。 */
    fun hasAsked(): Boolean

    fun markAsked()

    /** 上次从系统读到的授权结果；没记录过返回 null。 */
    fun lastKnownAllowed(): Boolean?

    fun setLastKnownAllowed(allowed: Boolean)
}
