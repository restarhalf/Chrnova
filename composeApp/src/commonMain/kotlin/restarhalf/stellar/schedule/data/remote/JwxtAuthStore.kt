package restarhalf.stellar.schedule.data.remote

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set

/**
 * 教务系统认证存储类
 * 
 * 负责存储教务系统的认证相关数据，包括：
 * - 访问令牌（Token）
 * - 用户信息（学号、姓名、班级、学院）
 * - 登录凭据（用于自动登录）
 * 
 * @param settings ObservableSettings实例
 */
class JwxtAuthStore(private val settings: ObservableSettings) {

    /**
     * 获取设置实例
     * 
     * @return ObservableSettings
     */
    internal fun getSettings(): ObservableSettings = settings

    /**
     * 获取访问令牌
     * 
     * @return 令牌字符串，为空时返回null
     */
    fun getToken(): String? = settings.getStringOrNull(KEY_TOKEN)?.takeIf { it.isNotBlank() }

    /**
     * 设置访问令牌
     * 
     * @param token 令牌字符串
     */
    fun setToken(token: String?) {
        settings[KEY_TOKEN] = token
    }

    /**
     * 获取服务器ID Cookie
     * 
     * @return Cookie字符串
     */
    fun getServerIdCookie(): String? =
        settings.getStringOrNull(KEY_SERVERID_COOKIE)?.takeIf { it.isNotBlank() }

    /**
     * 设置服务器ID Cookie
     * 
     * @param cookie Cookie字符串
     */
    fun setServerIdCookie(cookie: String?) {
        settings[KEY_SERVERID_COOKIE] = cookie
    }

    /** 清除所有认证数据 */
    fun clear() {
        settings.clear()
    }

    /** 清除会话数据（保留凭据） */
    fun clearSession() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_SERVERID_COOKIE)
        settings.remove(KEY_USER_NO)
        settings.remove(KEY_NAME)
        settings.remove(KEY_CLS_NAME)
        settings.remove(KEY_ACADEMY_NAME)
    }

    /** 清除令牌 */
    fun clearToken() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_SERVERID_COOKIE)
    }

    /**
     * 获取上次登录的学号
     * 
     * @return 学号
     */
    fun getLastUserNo(): String? =
        settings.getStringOrNull(KEY_LAST_USER_NO)?.takeIf { it.isNotBlank() }

    /**
     * 设置上次登录的学号
     * 
     * @param userNo 学号
     */
    fun setLastUserNo(userNo: String?) {
        settings[KEY_LAST_USER_NO] = userNo
    }

    /**
     * 获取学号
     * 
     * @return 学号
     */
    fun getUserNo(): String? = settings.getStringOrNull(KEY_USER_NO)?.takeIf { it.isNotBlank() }

    /**
     * 设置学号
     * 
     * @param userNo 学号
     */
    fun setUserNo(userNo: String?) {
        settings[KEY_USER_NO] = userNo
    }

    /**
     * 获取姓名
     * 
     * @return 姓名
     */
    fun getName(): String? = settings.getStringOrNull(KEY_NAME)?.takeIf { it.isNotBlank() }

    /**
     * 设置姓名
     * 
     * @param name 姓名
     */
    fun setName(name: String?) {
        settings[KEY_NAME] = name
    }

    /**
     * 获取班级名称
     * 
     * @return 班级名称
     */
    fun getClsName(): String? = settings.getStringOrNull(KEY_CLS_NAME)?.takeIf { it.isNotBlank() }

    /**
     * 设置班级名称
     * 
     * @param clsName 班级名称
     */
    fun setClsName(clsName: String?) {
        settings[KEY_CLS_NAME] = clsName
    }

    /**
     * 获取学院名称
     * 
     * @return 学院名称
     */
    fun getAcademyName(): String? =
        settings.getStringOrNull(KEY_ACADEMY_NAME)?.takeIf { it.isNotBlank() }

    /**
     * 设置学院名称
     * 
     * @param academyName 学院名称
     */
    fun setAcademyName(academyName: String?) {
        settings[KEY_ACADEMY_NAME] = academyName
    }

    /**
     * 设置用户档案
     * 
     * @param name 姓名
     * @param userNo 学号
     * @param clsName 班级名称
     * @param academyName 学院名称
     */
    fun setProfile(name: String?, userNo: String?, clsName: String?, academyName: String?) {
        settings[KEY_NAME] = name
        settings[KEY_USER_NO] = userNo
        settings[KEY_CLS_NAME] = clsName
        settings[KEY_ACADEMY_NAME] = academyName
    }

    /**
     * 设置登录凭据
     * 
     * @param userNo 学号
     * @param password 密码
     */
    fun setCredentials(userNo: String, password: String) {
        settings[KEY_SAVED_USER_NO] = userNo
        settings[KEY_SAVED_PASSWORD] = password
    }

    /**
     * 获取保存的登录凭据
     * 
     * @return 学号和密码的Pair，不存在时返回null
     */
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
