package restarhalf.stellar.schedule.domain.usecase

import kotlinx.serialization.SerializationException
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 带会话刷新的体育系统请求重试
 *
 * 请求因令牌过期或响应解析失败时，刷新会话后重试一次。
 *
 * @param peAuthWorkflow 体育系统认证工作流端口
 * @param fetch 请求操作
 */
internal suspend fun <T> withSessionRetry(
    peAuthWorkflow: PEAuthWorkflowPort,
    fetch: suspend () -> T,
): T {
    return try {
        fetch()
    } catch (e: Exception) {
        if (e is PETokenExpiredException || e is SerializationException) {
            peAuthWorkflow.refreshSession()
            fetch()
        } else {
            throw e
        }
    }
}
