package restarhalf.stellar.schedule.domain.port

interface ExactAlarmPermissionPort {

    /** 系统当前是否允许排精确闹钟；没有这项权限的平台恒为 true。 */
    fun isExactAlarmAllowed(): Boolean

    /** 打开系统的精确闹钟授权页；已经允许或平台不支持时返回 false。 */
    fun openExactAlarmSettings(): Boolean
}
