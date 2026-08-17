package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.PEProfile

/**
 * 体育系统认证端口接口
 *
 * 定义体育系统认证信息的抽象接口，由数据层实现。
 * 采用端口-适配器模式，解耦业务逻辑与具体实现。
 */
interface PEAuthPort {
    /** 观察认证令牌的变化 */
    fun observeToken(): Flow<String>

    /** 观察用户档案信息的变化 */
    fun observeProfile(): Flow<PEProfile>

    /**
     * 设置用户档案
     *
     * @param profile 用户档案
     */
    fun setProfile(profile: PEProfile)

    /** 设置用户凭据（用户名和密码） */
    fun setCredentials(username: String, password: String)

    /** 清除用户凭据和登录状态 */
    fun clear()
}
