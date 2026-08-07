package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import restarhalf.stellar.schedule.domain.port.CourseSelectionServicePort
import restarhalf.stellar.schedule.domain.port.ServiceLogEntry
import restarhalf.stellar.schedule.domain.port.SnatchServiceConfig

/**
 * iOS 后台抢课端口实现。
 *
 * iOS 系统不允许应用长时间后台运行网络请求，[isSupported] 始终返回 false，
 * [start] 直接返回 false，[stop] 为 no-op。iOS 用户仅能使用前台抢课。
 */
class CourseSelectionServicePortImpl : CourseSelectionServicePort {
    override val isSupported: Boolean = false

    private val _running = MutableStateFlow(false)
    override val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _latestLog = MutableStateFlow(ServiceLogEntry(""))
    override val latestLog: StateFlow<ServiceLogEntry> = _latestLog.asStateFlow()

    override fun start(config: SnatchServiceConfig): Boolean = false

    override fun stop() {
        // no-op
    }
}
