package restarhalf.stellar.schedule.data.remote

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set

/**
 * 体育系统认证存储类
 *
 * 负责存储体育系统的认证相关数据，包括：
 * - 访问令牌（Token）
 * - 用户标识（UserId）
 * - 用户档案（姓名、学号、测试码）
 * - 登录凭据（用于自动登录）
 *
 * @param settings ObservableSettings实例
 */
class PEAuthStore(private val settings: ObservableSettings) {

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
     * 获取用户标识
     *
     * @return 用户标识，为空时返回null
     */
    fun getUserId(): String? = settings.getStringOrNull(KEY_USER_ID)?.takeIf { it.isNotBlank() }

    /**
     * 设置用户标识
     *
     * @param userId 用户标识
     */
    fun setUserId(userId: String?) {
        settings[KEY_USER_ID] = userId
    }

    /** 清除所有认证数据 */
    fun clear() {
        settings.clear()
    }

    /** 清除会话数据（保留凭据） */
    fun clearSession() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_STU_NAME)
        settings.remove(KEY_STD_NUMBER)
        settings.remove(KEY_TEST_CODE)
    }

    /**
     * 获取上次登录的用户名
     *
     * @return 用户名
     */
    fun getLastUsername(): String? =
        settings.getStringOrNull(KEY_LAST_USERNAME)?.takeIf { it.isNotBlank() }

    /**
     * 设置上次登录的用户名
     *
     * @param username 用户名
     */
    fun setLastUsername(username: String?) {
        settings[KEY_LAST_USERNAME] = username
    }

    /**
     * 设置用户档案
     *
     * @param stuName 姓名
     * @param stdNumber 学号
     * @param testCode 测试码
     */
    fun setProfile(stuName: String?, stdNumber: String?, testCode: String?) {
        settings[KEY_STU_NAME] = stuName
        settings[KEY_STD_NUMBER] = stdNumber
        settings[KEY_TEST_CODE] = testCode
    }

    /**
     * 设置登录凭据
     *
     * @param username 用户名
     * @param password 密码
     */
    fun setCredentials(username: String, password: String) {
        settings[KEY_SAVED_USERNAME] = username
        settings[KEY_SAVED_PASSWORD] = password
    }

    /**
     * 获取保存的登录凭据
     *
     * @return 用户名和密码的Pair，不存在时返回null
     */
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
