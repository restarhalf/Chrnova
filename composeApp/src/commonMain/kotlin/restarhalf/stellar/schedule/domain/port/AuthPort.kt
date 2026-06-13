package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.AuthProfile

/**
 * 认证端口接口
 * 
 * 定义用户认证相关的抽象接口，由数据层实现。
 * 采用端口-适配器模式，解耦业务逻辑与具体实现。
 */
interface AuthPort {
    /** 观察认证令牌的变化 */
    fun observeToken(): Flow<String>
    /** 观察用户档案信息的变化 */
    fun observeProfile(): Flow<AuthProfile>

    /** 设置用户凭据（学号和密码） */
    fun setCredentials(userNo: String, password: String)
    /** 清除用户凭据和登录状态 */
    fun clear()
}
