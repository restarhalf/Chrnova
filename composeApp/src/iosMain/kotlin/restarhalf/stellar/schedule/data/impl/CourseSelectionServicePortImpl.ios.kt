package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import restarhalf.stellar.schedule.domain.port.CourseSelectionServicePort
import restarhalf.stellar.schedule.domain.port.ServiceLogEntry
import restarhalf.stellar.schedule.domain.port.SnatchServiceConfig

class CourseSelectionServicePortImpl : CourseSelectionServicePort {
    override val isSupported: Boolean = false

    private val _running = MutableStateFlow(false)
    override val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _latestLog = MutableStateFlow(ServiceLogEntry(""))
    override val latestLog: StateFlow<ServiceLogEntry> = _latestLog.asStateFlow()

    override fun start(config: SnatchServiceConfig): Boolean = false

    override fun stop() {

    }
}
