package restarhalf.stellar.schedule.core.update

import kotlinx.coroutines.flow.StateFlow

/**
 * 应用更新端口接口
 * 
 * 定义应用更新相关的抽象接口，包括版本检查、下载、安装等。
 * 同时提供QQ群和微信赞赏码等辅助功能。
 */
interface AppUpdatePort {
    /** APK下载状态流，用于观察下载进度 */
    val apkDownloadState: StateFlow<ApkDownloadState>

    /**
     * 检查是否有新版本
     * 
     * @param currentVersionName 当前应用版本号
     * @return 如果有新版本返回更新信息，否则返回null
     */
    suspend fun check(currentVersionName: String): AppUpdateInfo?

    /**
     * 开始直接下载APK
     * 
     * @param info 应用更新信息，包含下载链接
     */
    fun startDirectDownload(info: AppUpdateInfo)

    /** 取消APK下载 */
    fun cancelApkDownload()

    /** 检查是否可以请求安装未知应用权限（Android 8.0+） */
    fun canRequestInstallPackages(): Boolean

    /** 打开未知应用安装权限设置页面 */
    fun openUnknownSourcesSettings()

    /**
     * 启动安装程序
     * 
     * @param apkPath APK文件路径
     * @return 是否成功启动安装
     */
    fun launchInstaller(apkPath: String): Boolean

    /**
     * 保存微信赞赏码到相册
     * 
     * @return 是否保存成功
     */
    fun saveWxpayToPictures(): Boolean

    /**
     * 加入QQ群
     * 
     * @param key QQ群加群链接中的key参数
     * @return 是否成功启动QQ
     */
    fun joinQqGroup(key: String?): Boolean

    /**
     * 直接打开微信扫一扫
     * 
     * @return 是否成功启动微信
     */
    fun openWeChatScanDirect(): Boolean
}
