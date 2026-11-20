package restarhalf.stellar.schedule.domain.port

/** 提醒相关系统权限的可查询快照，供提醒设置页展示授权状态。 */
data class ReminderPermissionState(
    val notificationGranted: Boolean = false,
    val exactAlarmGranted: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false,
    val dndGranted: Boolean = false,
    val islandSupported: Boolean = false,
    /** 真·超级岛（FocusTier.ISLAND），决定测试按钮发岛还是发实况。 */
    val islandTierSupported: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuAuthorized: Boolean = false,
    /** 该平台有「测试提醒」能力（Android 才有，iOS 不画测试按钮）。 */
    val canSendTestNotification: Boolean = false,
)

interface ReminderPermissionPort {

    fun snapshot(): ReminderPermissionState

    fun openBatteryOptimizationSettings(): Boolean

    fun openAppDetailsSettings(): Boolean

    fun openDndPolicySettings(): Boolean

    /** 申请 Shizuku 授权；没有 Shizuku 能力时直接回调 false。 */
    fun requestShizukuPermission(onResult: (Boolean) -> Unit) {
        onResult(false)
    }

    /** 发送一条测试提醒（超级岛或实况）。 */
    fun sendTestNotification(island: Boolean) {}
}
