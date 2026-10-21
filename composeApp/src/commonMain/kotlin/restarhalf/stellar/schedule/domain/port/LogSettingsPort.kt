package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow


interface LogSettingsPort {

    fun observeLogEnabled(): Flow<Boolean>

    fun setLogEnabled(enabled: Boolean)
}
