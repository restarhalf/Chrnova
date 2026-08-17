package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getStringFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import restarhalf.stellar.schedule.data.remote.PEAuthStore
import restarhalf.stellar.schedule.domain.model.PEProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort

/**
 * 体育系统认证端口实现类
 *
 * 实现PEAuthPort接口，负责体育系统认证信息的存储和读取。
 * 使用PEAuthStore进行体育系统认证数据的持久化。
 *
 * @param authStore 体育系统认证存储
 */
@OptIn(ExperimentalSettingsApi::class)
class PEAuthPortImpl(
    private val authStore: PEAuthStore,
) : PEAuthPort {

    /**
     * 观察认证令牌变化
     *
     * @return 令牌Flow
     */
    override fun observeToken(): Flow<String> {
        return authStore.getSettings().getStringFlow(PEAuthStore.KEY_TOKEN, "")
    }

    /**
     * 观察用户档案变化
     *
     * @return 用户档案Flow
     */
    override fun observeProfile(): Flow<PEProfile> {
        val settings = authStore.getSettings()
        return combine(
            settings.getStringFlow(PEAuthStore.KEY_STU_NAME, ""),
            settings.getStringFlow(PEAuthStore.KEY_STD_NUMBER, ""),
            settings.getStringFlow(PEAuthStore.KEY_TEST_CODE, "")
        ) { stuName, stdNumber, testCode ->
            PEProfile(
                stuName = stuName, stdNumber = stdNumber, testCode = testCode
            )
        }
    }

    /**
     * 设置用户档案
     *
     * @param profile 用户档案
     */
    override fun setProfile(profile: PEProfile) {
        authStore.setProfile(
            stuName = profile.stuName,
            stdNumber = profile.stdNumber,
            testCode = profile.testCode
        )
    }

    /**
     * 设置用户凭据
     *
     * @param username 用户名
     * @param password 密码
     */
    override fun setCredentials(username: String, password: String) {
        authStore.setCredentials(username, password)
    }

    /** 清除所有认证数据 */
    override fun clear() {
        authStore.clear()
    }
}
