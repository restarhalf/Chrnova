package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.platform.AppIoDispatcher

class AuthWorkflowPortImpl(
    private val gateway: JwxtGateway,
    private val authStore: JwxtAuthStore,
    private val courseRepository: CourseRepository,
) : AuthWorkflowPort {

    override suspend fun ensureLoggedIn() {
        val token = authStore.getToken().orEmpty()
        if (token.isNotBlank()) return

        val (userNo, password) = authStore.getCredentials()
            ?: throw IllegalStateException("请先登录")
        login(userNo = userNo, password = password)
    }

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

        val newUserNo = resp.data?.userNo.orEmpty().trim().ifBlank { userNo.trim() }
        if (oldUserNo.isNotBlank() && newUserNo.isNotBlank() && oldUserNo != newUserNo) {
            withContext(AppIoDispatcher) { courseRepository.clearAllCourses() }
        }

        authStore.setLastUserNo(newUserNo)

        authStore.setProfile(
            name = resp.data?.name,
            userNo = resp.data?.userNo,
            clsName = resp.data?.clsName,
            academyName = resp.data?.academyName
        )

        authStore.setCredentials(userNo, password)
    }

    override fun logout() {
        authStore.clearSession()
    }
}
