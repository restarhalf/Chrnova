package restarhalf.stellar.schedule.domain.port


interface PEAuthWorkflowPort {
    
    suspend fun ensureLoggedIn()

    
    suspend fun login(
        username: String,
        password: String,
    )

    
    fun logout()

    
    suspend fun refreshSession()
}
