package restarhalf.stellar.schedule.core.update

/**
 * 应用更新端口接口
 *
 * 定义应用更新相关的抽象接口，包括版本检查和跳转下载。
 * 同时提供QQ群和微信赞赏码等辅助功能。
 */
interface AppUpdatePort {
    /**
     * 检查是否有新版本，默认从 Cloudflare Worker 获取
     *
     * @param currentVersionName 当前版本名称
     * @param grayUid 灰度标识（学号 SHA-256 hex），为 null 时不参与灰度，仅获取正式版
     */
    suspend fun check(currentVersionName: String, grayUid: String? = null): AppUpdateInfo? =
        checkUpdateFromWorker(currentVersionName, grayUid)

    /** 跳转到浏览器下载 */
    fun startDirectDownload(info: AppUpdateInfo)

    /** 保存微信赞赏码到相册 */
    fun saveWxpayToPictures(): Boolean = false

    /** 加入QQ群 */
    fun joinQqGroup(key: String?): Boolean = false

    /** 直接打开微信扫一扫 */
    fun openWeChatScanDirect(): Boolean = false
}
