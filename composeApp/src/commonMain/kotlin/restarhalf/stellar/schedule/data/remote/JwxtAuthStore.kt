package restarhalf.stellar.schedule.data.remote

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set

class JwxtAuthStore(private val settings: ObservableSettings) {

    internal fun getSettings(): ObservableSettings = settings

    fun getToken(): String? = settings.getStringOrNull(KEY_TOKEN)?.takeIf { it.isNotBlank() }

    fun setToken(token: String?) {
        settings[KEY_TOKEN] = token
    }

    fun getServerIdCookie(): String? =
        settings.getStringOrNull(KEY_SERVERID_COOKIE)?.takeIf { it.isNotBlank() }

    fun setServerIdCookie(cookie: String?) {
        settings[KEY_SERVERID_COOKIE] = cookie
    }

    fun clear() {
        settings.clear()
    }

    fun clearSession() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_SERVERID_COOKIE)
        settings.remove(KEY_USER_NO)
        settings.remove(KEY_NAME)
        settings.remove(KEY_CLS_NAME)
        settings.remove(KEY_ACADEMY_NAME)
    }

    fun getLastUserNo(): String? =
        settings.getStringOrNull(KEY_LAST_USER_NO)?.takeIf { it.isNotBlank() }

    fun setLastUserNo(userNo: String?) {
        settings[KEY_LAST_USER_NO] = userNo
    }

    fun getUserNo(): String? = settings.getStringOrNull(KEY_USER_NO)?.takeIf { it.isNotBlank() }

    fun setUserNo(userNo: String?) {
        settings[KEY_USER_NO] = userNo
    }

    fun getName(): String? = settings.getStringOrNull(KEY_NAME)?.takeIf { it.isNotBlank() }

    fun setName(name: String?) {
        settings[KEY_NAME] = name
    }

    fun getClsName(): String? = settings.getStringOrNull(KEY_CLS_NAME)?.takeIf { it.isNotBlank() }

    fun setClsName(clsName: String?) {
        settings[KEY_CLS_NAME] = clsName
    }

    fun getAcademyName(): String? =
        settings.getStringOrNull(KEY_ACADEMY_NAME)?.takeIf { it.isNotBlank() }

    fun setAcademyName(academyName: String?) {
        settings[KEY_ACADEMY_NAME] = academyName
    }

    fun setProfile(name: String?, userNo: String?, clsName: String?, academyName: String?) {
        settings[KEY_NAME] = name
        settings[KEY_USER_NO] = userNo
        settings[KEY_CLS_NAME] = clsName
        settings[KEY_ACADEMY_NAME] = academyName
    }

    fun setCredentials(userNo: String, password: String) {
        settings[KEY_SAVED_USER_NO] = userNo
        settings[KEY_SAVED_PASSWORD] = password
    }

    fun getCredentials(): Pair<String, String>? {
        val userNo =
            settings.getStringOrNull(KEY_SAVED_USER_NO)?.takeIf { it.isNotBlank() } ?: return null
        val password =
            settings.getStringOrNull(KEY_SAVED_PASSWORD)?.takeIf { it.isNotBlank() } ?: return null
        return Pair(userNo, password)
    }

    companion object {
        internal const val PREFS_NAME = "jwxt_auth"
        internal const val KEY_TOKEN = "token"
        internal const val KEY_SERVERID_COOKIE = "serverid_cookie"
        internal const val KEY_USER_NO = "user_no"
        internal const val KEY_NAME = "name"
        internal const val KEY_CLS_NAME = "cls_name"
        internal const val KEY_ACADEMY_NAME = "academy_name"
        internal const val KEY_LAST_USER_NO = "last_user_no"
        internal const val KEY_SAVED_USER_NO = "saved_user_no"
        internal const val KEY_SAVED_PASSWORD = "saved_password"
    }
}
