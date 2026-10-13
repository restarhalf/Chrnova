package restarhalf.stellar.schedule.core.update


interface AppUpdatePort {
    
    suspend fun check(currentVersionName: String, grayUid: String? = null): AppUpdateInfo? =
        checkUpdateFromWorker(currentVersionName, grayUid)

    
    fun startDirectDownload(info: AppUpdateInfo)

    
    fun saveWxpayToPictures(): Boolean = false

    
    fun joinQqGroup(key: String?): Boolean = false

    
    fun openWeChatScanDirect(): Boolean = false
}
