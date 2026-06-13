package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 认证工作流端口实现类
 * 
 * 实现AuthWorkflowPort接口，负责用户认证流程的完整处理。
 * 包括登录、登出、会话刷新等操作。
 * 
 * @param gateway 教务系统网关客户端
 * @param authStore 教务系统认证存储
 * @param courseRepository 课程仓库
 */
class AuthWorkflowPortImpl(
    private val gateway: JwxtGateway,
    private val authStore: JwxtAuthStore,
    private val courseRepository: CourseRepository,
) : AuthWorkflowPort {

    /**
     * 确保用户已登录
     * 
     * 如果已有有效令牌，直接返回；否则使用保存的凭据登录。
     * 
     * @throws IllegalStateException 未保存凭据时抛出
     */
    override suspend fun ensureLoggedIn() {
        val token = authStore.getToken().orEmpty()
        if (token.isNotBlank()) return

        val (userNo, password) = authStore.getCredentials()
            ?: throw IllegalStateException("请先登录")
        login(userNo = userNo, password = password)
    }

    /**
     * 用户登录
     * 
     * 如果登录用户与之前不同，会清除本地课程数据。
     * 
     * @param userNo 学号
     * @param password 密码
     * @param captchaData 验证码数据
     * @param codeVal 用户输入的验证码
     * @param p 加密参数
     * @throws IllegalStateException 登录失败时抛出
     */
    override suspend fun login(
        userNo: String,
        password: String,
        captchaData: String,
        codeVal: String,
        p: String?,
    ) {
        val oldUserNo = authStore.getLastUserNo().orEmpty().trim()
        val resp = gateway.login(
            userNo = userNo,
            password = password,
            captchaData = captchaData,
            codeVal = codeVal,
            p = p
        )

        if (resp.code != 1) {
            throw IllegalStateException(resp.messageOrEmpty().ifBlank { "登录失败" })
        }

        val token = authStore.getToken().orEmpty()
        if (token.isBlank()) {
            throw IllegalStateException("登录成功但未获取到 Token")
        }

        // 如果用户切换，清除旧用户的课程数据
        val newUserNo = resp.data?.userNo.orEmpty().trim().ifBlank { userNo.trim() }
        if (oldUserNo.isNotBlank() && newUserNo.isNotBlank() && oldUserNo != newUserNo) {
            withContext(AppIoDispatcher) { courseRepository.clearAllCourses() }
        }

        authStore.setLastUserNo(newUserNo)

        // 保存用户档案
        authStore.setProfile(
            name = resp.data?.name,
            userNo = resp.data?.userNo,
            clsName = resp.data?.clsName,
            academyName = resp.data?.academyName
        )

        authStore.setCredentials(userNo, password)
    }

    /** 用户登出，清除会话 */
    override fun logout() {
        authStore.clearSession()
    }

    /** 刷新会话，重新登录 */
    override suspend fun refreshSession() {
        authStore.clearToken()
        ensureLoggedIn()
    }
}
