package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.AuthProfile

interface AuthPort {
    fun observeToken(): Flow<String>
    fun observeProfile(): Flow<AuthProfile>

    fun setCredentials(userNo: String, password: String)
    fun clear()
}
