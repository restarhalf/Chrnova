package restarhalf.stellar.schedule.ui.port

/**
 * 扫码展示类页面的屏幕控制端口。
 *
 * 二维码出示场景需要：屏幕保持常亮（防止展示中途锁屏）
 * 以及临时拉满亮度（保证户外强光下也能被扫描）。
 */
interface ScreenTunerPort {
    /**
     * 进入扫码展示模式：屏幕常亮 + 最高亮度。
     *
     * @return 是否成功生效（拿不到窗口/屏幕时返回 false，UI 可忽略）
     */
    fun enterScanPresentation(): Boolean

    /** 退出扫码展示模式：恢复原有亮度与系统熄屏策略 */
    fun exitScanPresentation()
}
