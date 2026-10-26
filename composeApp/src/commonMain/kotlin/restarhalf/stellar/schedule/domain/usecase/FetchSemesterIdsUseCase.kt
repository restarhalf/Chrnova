package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort

class FetchSemesterIdsUseCase(
    private val authWorkflow: JwxtAuthWorkflowPort,
    private val academic: AcademicPort,
    private val settings: TermSettingsPort,
) {

    suspend operator fun invoke(): List<String> {

        return try {
            val networkIds = fetchFromNetwork()
            if (networkIds.isNotEmpty()) {

                settings.setCachedSemesterIds(networkIds)
                networkIds
            } else {

                val cachedIds = settings.observeCachedSemesterIds().first()
                cachedIds.ifEmpty { networkIds }
            }
        } catch (e: Exception) {

            val cachedIds = settings.observeCachedSemesterIds().first()
            if (cachedIds.isNotEmpty()) {
                cachedIds
            } else {

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
