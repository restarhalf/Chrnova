package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.error.PETokenExpiredException
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

internal suspend fun <T> withSessionRetry(
    peAuthWorkflow: PEAuthWorkflowPort,
    replayOnAuthError: Boolean = true,
    fetch: suspend () -> T,
): T {
    return try {
        fetch()
    } catch (e: PETokenExpiredException) {
        peAuthWorkflow.refreshSession()
        if (replayOnAuthError) fetch() else throw e
    }
}
