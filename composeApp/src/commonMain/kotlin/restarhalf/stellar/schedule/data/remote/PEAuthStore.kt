package restarhalf.stellar.schedule.data.remote

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set


class PEAuthStore(private val settings: ObservableSettings) {

    
    internal fun getSettings(): ObservableSettings = settings

    
    fun getToken(): String? = settings.getStringOrNull(KEY_TOKEN)?.takeIf { it.isNotBlank() }

    
    fun setToken(token: String?) {
        settings[KEY_TOKEN] = token
    }

    
    fun getUserId(): String? = settings.getStringOrNull(KEY_USER_ID)?.takeIf { it.isNotBlank() }

    
    fun setUserId(userId: String?) {
        settings[KEY_USER_ID] = userId
    }

    
    fun clear() {
        settings.clear()
    }

    
    fun clearSession() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_STU_NAME)
        settings.remove(KEY_STD_NUMBER)
        settings.remove(KEY_TEST_CODE)
    }

    
    fun getLastUsername(): String? =
        settings.getStringOrNull(KEY_LAST_USERNAME)?.takeIf { it.isNotBlank() }

    
    fun setLastUsername(username: String?) {
        settings[KEY_LAST_USERNAME] = username
    }

    
    fun setProfile(stuName: String?, stdNumber: String?, testCode: String?) {
        settings[KEY_STU_NAME] = stuName
        settings[KEY_STD_NUMBER] = stdNumber
        settings[KEY_TEST_CODE] = testCode
    }

    
    fun getStdNumber(): String? =
        settings.getStringOrNull(KEY_STD_NUMBER)?.takeIf { it.isNotBlank() }

    
    fun setCredentials(username: String, password: String) {
        settings[KEY_SAVED_USERNAME] = username
        settings[KEY_SAVED_PASSWORD] = password
    }

    
    fun getCredentials(): Pair<String, String>? {
        val username =
            settings.getStringOrNull(KEY_SAVED_USERNAME)?.takeIf { it.isNotBlank() } ?: return null
        val password =
            settings.getStringOrNull(KEY_SAVED_PASSWORD)?.takeIf { it.isNotBlank() } ?: return null
        return Pair(username, password)
    }

    companion object {
        internal const val PREFS_NAME = "pe_auth"
        internal const val KEY_TOKEN = "pe_token"
        internal const val KEY_USER_ID = "pe_user_id"
        internal const val KEY_STU_NAME = "pe_stu_name"
        internal const val KEY_STD_NUMBER = "pe_std_number"
        internal const val KEY_TEST_CODE = "pe_test_code"
        internal const val KEY_LAST_USERNAME = "pe_last_username"
        internal const val KEY_SAVED_USERNAME = "pe_username"
        internal const val KEY_SAVED_PASSWORD = "pe_password"
    }
}
