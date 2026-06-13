package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.remote.PEAuthStore
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEDetailResponse
import restarhalf.stellar.schedule.data.remote.PELoginResponse
import restarhalf.stellar.schedule.data.remote.PEScoreListResponse
import restarhalf.stellar.schedule.data.remote.PEStudentInfo
import restarhalf.stellar.schedule.data.remote.PEStudentInfoResponse
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.data.repository.PERepository
import restarhalf.stellar.schedule.data.repository.PERoomRepository

/**
 * 体育成绩用例
 * 
 * 封装体育系统相关的业务逻辑，包括登录、成绩查询、学生信息获取等。
 * 支持自动重试和本地缓存。
 */
class PEUseCase(
    private val repository: PERepository,
    private val authStore: PEAuthStore,
    private val roomRepository: PERoomRepository? = null
) {
    /**
     * 检查是否已登录
     * 
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean = authStore.getToken() != null

    /**
     * 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录响应
     */
    suspend fun login(username: String, password: String): PELoginResponse =
        repository.login(username, password)

    /**
     * 观察成绩列表（本地缓存）
     * 
     * @return 成绩列表Flow
     */
    fun observeScoreList(): Flow<List<PEYearScore>> =
        roomRepository?.observeAllScores() ?: throw IllegalStateException("本地缓存不可用")

    /**
     * 获取成绩列表
     * 
     * @return 成绩列表响应
     */
    suspend fun getScoreList(): PEScoreListResponse =
        withAutoRetryAndCache(
            fetch = { repository.getScoreList() },
            onSuccess = { response -> roomRepository?.replaceScores(response.dataArr) }
        )

    /**
     * 获取成绩详情
     * 
     * @param schoolYear 学年
     * @return 成绩详情响应
     */
    suspend fun getScoreDetail(schoolYear: String): PEDetailResponse =
        withAutoRetryAndCache(
            fetch = { repository.getScoreDetail(schoolYear) },
            onSuccess = { response -> response.data?.let { roomRepository?.saveDetailData(schoolYear, it) } }
        )

    /**
     * 获取学生信息
     * 
     * @return 学生信息响应
     */
    suspend fun getStudentInfo(): PEStudentInfoResponse =
        withAutoRetryAndCache(
            fetch = { repository.getStudentInfo() },
            onSuccess = { response -> response.data?.let { roomRepository?.saveStudentInfo(it) } }
        )

    /**
     * 用户登出
     */
    suspend fun logout() {
        authStore.clearAll()
        roomRepository?.clearAll()
    }

    fun observeStudentInfo(): Flow<PEStudentInfo?> =
        roomRepository?.observeStudentInfo() ?: throw IllegalStateException("本地缓存不可用")

    fun observeDetailData(schoolYear: String): Flow<PEDetailData?> =
        roomRepository?.observeDetailData(schoolYear) ?: throw IllegalStateException("本地缓存不可用")

    private suspend fun <T> withAutoRetry(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: PETokenExpiredException) {
            val username = authStore.getUsername()
            val password = authStore.getPassword()
            if (username != null && password != null) {
                val result = repository.login(username, password)
                if (result.status == "PASS") {
                    block()
                } else {
                    authStore.clear()
                    throw PETokenExpiredException("登录已过期，请重新登录")
                }
            } else {
                authStore.clear()
                throw PETokenExpiredException("登录已过期，请重新登录")
            }
        }
    }

    private suspend fun <T> withAutoRetryAndCache(
        fetch: suspend () -> T,
        onSuccess: (suspend (T) -> Unit)? = null
    ): T {
        return try {
            val result = fetch()
            onSuccess?.invoke(result)
            result
        } catch (e: PETokenExpiredException) {
            val username = authStore.getUsername()
            val password = authStore.getPassword()
            if (username != null && password != null) {
                val loginResult = repository.login(username, password)
                if (loginResult.status == "PASS") {
                    val result = fetch()
                    onSuccess?.invoke(result)
                    result
                } else {
                    authStore.clear()
                    throw PETokenExpiredException("登录已过期，请重新登录")
                }
            } else {
                authStore.clear()
                throw PETokenExpiredException("登录已过期，请重新登录")
            }
        }
    }
}
