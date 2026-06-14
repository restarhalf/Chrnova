package restarhalf.stellar.schedule.data.remote

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import restarhalf.stellar.schedule.domain.port.PEAuthPort

class PEAuthStore(private val settings: ObservableSettings) : PEAuthPort {

    override fun getToken(): String? = settings.getStringOrNull(KEY_TOKEN)?.takeIf { it.isNotBlank() }

    fun setToken(token: String?) {
        settings[KEY_TOKEN] = token
    }

    fun getUserId(): String? = settings.getStringOrNull(KEY_USER_ID)?.takeIf { it.isNotBlank() }

    fun setUserId(userId: String?) {
        settings[KEY_USER_ID] = userId
    }

    override fun getUsername(): String? = settings.getStringOrNull(KEY_USERNAME)?.takeIf { it.isNotBlank() }

    fun setUsername(username: String?) {
        settings[KEY_USERNAME] = username
    }

    override fun getPassword(): String? = settings.getStringOrNull(KEY_PASSWORD)?.takeIf { it.isNotBlank() }

    fun setPassword(password: String?) {
        settings[KEY_PASSWORD] = password
    }

    override fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER_ID)
    }

    override fun clearAll() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_USERNAME)
        settings.remove(KEY_PASSWORD)
    }

    companion object {
        private const val KEY_TOKEN = "pe_token"
        private const val KEY_USER_ID = "pe_user_id"
        private const val KEY_USERNAME = "pe_username"
        private const val KEY_PASSWORD = "pe_password"
    }
}
