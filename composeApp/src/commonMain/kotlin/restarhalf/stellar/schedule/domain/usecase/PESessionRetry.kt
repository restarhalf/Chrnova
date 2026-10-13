package restarhalf.stellar.schedule.domain.usecase

import kotlinx.serialization.SerializationException
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort


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
