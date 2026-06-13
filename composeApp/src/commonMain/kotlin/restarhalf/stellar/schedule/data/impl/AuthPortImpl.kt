package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getStringFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.domain.model.AuthProfile
import restarhalf.stellar.schedule.domain.port.AuthPort

/**
 * 认证端口实现类
 * 
 * 实现AuthPort接口，负责用户认证信息的存储和读取。
 * 使用JwxtAuthStore进行教务系统认证数据的持久化。
 * 
 * @param authStore 教务系统认证存储
 */
@OptIn(ExperimentalSettingsApi::class)
class AuthPortImpl(
    private val authStore: JwxtAuthStore,
) : AuthPort {

    /**
     * 观察认证令牌变化
     * 
     * @return 令牌Flow
     */
    override fun observeToken(): Flow<String> {
        return authStore.getSettings().getStringFlow(JwxtAuthStore.KEY_TOKEN, "")
    }

    /**
     * 观察用户档案变化
     * 
     * @return 用户档案Flow
     */
    override fun observeProfile(): Flow<AuthProfile> {
        val settings = authStore.getSettings()
        return combine(
            settings.getStringFlow(JwxtAuthStore.KEY_NAME, ""),
            settings.getStringFlow(JwxtAuthStore.KEY_USER_NO, ""),
            settings.getStringFlow(JwxtAuthStore.KEY_CLS_NAME, ""),
            settings.getStringFlow(JwxtAuthStore.KEY_ACADEMY_NAME, "")
        ) { name, userNo, clsName, academyName ->
            AuthProfile(
                name = name, userNo = userNo, clsName = clsName, academyName = academyName
            )
        }
    }

    /**
     * 设置用户凭据
     * 
     * @param userNo 学号
     * @param password 密码
     */
    override fun setCredentials(userNo: String, password: String) {
        authStore.setCredentials(userNo, password)
    }

    /** 清除所有认证数据 */
    override fun clear() {
        authStore.clear()
    }
}
