package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.PEScoreListResponse
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.data.repository.PERepository
import restarhalf.stellar.schedule.data.repository.PERoomRepository
import restarhalf.stellar.schedule.domain.port.PEAuthPort

/**
 * 体育成绩列表用例
 */
class PEScoreListUseCase(
    private val repository: PERepository,
    private val peAuth: PEAuthPort,
    private val roomRepository: PERoomRepository? = null,
) {
    /**
     * 观察成绩列表（本地缓存）
     */
    fun observeScoreList(): Flow<List<PEYearScore>> =
        roomRepository?.observeAllScores() ?: throw IllegalStateException("本地缓存不可用")

    /**
     * 获取成绩列表
     */
    suspend operator fun invoke(): PEScoreListResponse {
        return withAutoRetryAndCache(
            fetch = { repository.getScoreList() },
            onSuccess = { response -> roomRepository?.replaceScores(response.dataArr) },
        )
    }

    private suspend fun <T> withAutoRetryAndCache(
        fetch: suspend () -> T,
        onSuccess: (suspend (T) -> Unit)? = null,
    ): T {
        return try {
            val result = fetch()
            onSuccess?.invoke(result)
            result
        } catch (e: PETokenExpiredException) {
            val username = peAuth.getUsername()
            val password = peAuth.getPassword()
            if (username != null && password != null) {
                val loginResult = repository.login(username, password)
                if (loginResult.status == "PASS") {
                    val result = fetch()
                    onSuccess?.invoke(result)
                    result
                } else {
                    AppLogger.log("PE", "自动重新登录失败: ${loginResult.message}", e)
                    peAuth.clear()
                    throw PETokenExpiredException("登录已过期，请重新登录")
                }
            } else {
                AppLogger.log("PE", "无存储凭证，无法自动重新登录", e)
                peAuth.clear()
                throw PETokenExpiredException("登录已过期，请重新登录")
            }
        }
    }
}