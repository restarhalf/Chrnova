package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * 体育系统认证端口
 *
 * 提供体育系统的登录凭证管理能力。
 */
interface PEAuthPort {
    /** 获取当前token */
    fun getToken(): String?

    /** 观察token变化 */
    fun observeToken(): Flow<String?>

    /** 获取保存的用户名 */
    fun getUsername(): String?

    /** 获取保存的密码 */
    fun getPassword(): String?

    /** 清除当前token */
    fun clear()

    /** 清除所有凭证 */
    fun clearAll()
}
