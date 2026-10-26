package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.PEAuthProfile

interface PEAuthPort {
    
    fun observeToken(): Flow<String>

    fun observeProfile(): Flow<PEAuthProfile>

    fun setProfile(profile: PEAuthProfile)

    fun setCredentials(username: String, password: String)

    fun clear()

    fun getToken(): String?

    fun getUserId(): String?

    fun getStdNumber(): String?
}
