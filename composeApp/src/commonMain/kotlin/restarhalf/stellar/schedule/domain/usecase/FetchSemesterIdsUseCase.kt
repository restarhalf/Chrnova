package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort

/**
 * 获取学期ID列表用例
 *
 * 从教务系统获取所有可用的学期ID列表。
 */
class FetchSemesterIdsUseCase(
    private val authWorkflow: JwxtAuthWorkflowPort,
    private val academic: AcademicPort,
    private val settings: SettingsPort,
) {
    /**
     * 获取学期ID列表
     *
     * @return 学期ID列表
     */
    suspend operator fun invoke(): List<String> {
        // 先尝试网络请求
        return try {
            val networkIds = fetchFromNetwork()
            if (networkIds.isNotEmpty()) {
                // 成功获取，更新缓存
                settings.setCachedSemesterIds(networkIds)
                networkIds
            } else {
                // 网络返回空列表，尝试从缓存获取
                val cachedIds = settings.observeCachedSemesterIds().first()
                cachedIds.ifEmpty { networkIds }
            }
        } catch (e: Exception) {
            // 网络请求失败，尝试从缓存获取
            val cachedIds = settings.observeCachedSemesterIds().first()
            if (cachedIds.isNotEmpty()) {
                cachedIds
            } else {
                // 缓存也为空，抛出异常
                throw e
            }
        }
    }

    private suspend fun fetchFromNetwork(): List<String> {
        val firstAttempt = runCatching { academic.fetchSemesterIds() }
        if (firstAttempt.isSuccess) return firstAttempt.getOrThrow()

        val ex = firstAttempt.exceptionOrNull()
        if (ex?.isNetworkError() == true) {
            AppLogger.log("Fetch", "获取学期列表网络错误", ex)
            return emptyList()
        }

        ex?.let { AppLogger.log("Fetch", "获取学期列表失败，刷新会话重试", it) }
        authWorkflow.refreshSession()
        return runCatching { academic.fetchSemesterIds() }
            .onFailure { AppLogger.log("Fetch", "获取学期列表重试失败", it) }
            .getOrElse { emptyList() }
    }
}
