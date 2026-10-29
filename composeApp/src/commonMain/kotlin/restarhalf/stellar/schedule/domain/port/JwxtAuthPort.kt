package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.JwxtSessionSnapshot

interface JwxtAuthPort {
    
    fun observeToken(): Flow<String>
    
    fun observeProfile(): Flow<JwxtAuthProfile>

    fun getSessionSnapshot(): JwxtSessionSnapshot

    fun setCredentials(userNo: String, password: String)
    
    fun clear()
}
