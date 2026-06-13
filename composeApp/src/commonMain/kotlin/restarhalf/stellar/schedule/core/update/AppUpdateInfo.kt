package restarhalf.stellar.schedule.core.update

/**
 * 应用更新信息数据类
 * 
 * 存储从GitHub Release获取的最新版本信息。
 */
data class AppUpdateInfo(
    /** 最新版本号（如"1.2.0"） */
    val latestVersion: String,
    /** 发布页面URL，用于在浏览器中打开 */
    val releasePageUrl: String,
    /** 下载链接，用于直接下载APK/IPA */
    val downloadUrl: String,
    /** 更新日志，描述本次更新的内容 */
    val changelog: String,
)
