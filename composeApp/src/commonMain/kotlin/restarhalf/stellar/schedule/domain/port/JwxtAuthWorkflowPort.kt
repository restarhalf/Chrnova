package restarhalf.stellar.schedule.domain.port


interface JwxtAuthWorkflowPort {
    
    suspend fun ensureLoggedIn()

    
    suspend fun login(
        userNo: String,
        password: String,
        captchaData: String = "",
        codeVal: String = "",
        p: String? = null,
    )

    
    fun logout()

    
    suspend fun refreshSession()
}
