package restarhalf.stellar.schedule.data.impl

import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import restarhalf.stellar.schedule.CourseSelectionService
import restarhalf.stellar.schedule.domain.port.CourseSelectionServicePort
import restarhalf.stellar.schedule.domain.port.ServiceLogEntry
import restarhalf.stellar.schedule.domain.port.SnatchServiceConfig

/**
 * Android 后台抢课端口实现。
 *
 * 通过启动 [CourseSelectionService] Foreground Service 实现真正后台抢课。
 * 状态通过 Service 的静态字段同步给本端口。
 */
class CourseSelectionServicePortImpl(
    private val context: Context,
) : CourseSelectionServicePort {

    override val isSupported: Boolean = true

    private val _running: MutableStateFlow<Boolean> = CourseSelectionService.runningFlow
    override val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _latestLog: MutableStateFlow<ServiceLogEntry> = CourseSelectionService.latestLogFlow
    override val latestLog: StateFlow<ServiceLogEntry> = _latestLog.asStateFlow()

    override fun start(config: SnatchServiceConfig): Boolean {
        if (_running.value) return true
        CourseSelectionService.pendingConfig = config
        val intent = Intent(context, CourseSelectionService::class.java).apply {
            action = CourseSelectionService.ACTION_START
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            true
        } catch (e: Exception) {
            // 部分厂商后台启动限制可能抛 IllegalStateException
            CourseSelectionService.pendingConfig = null
            false
        }
    }

    override fun stop() {
        val intent = Intent(context, CourseSelectionService::class.java).apply {
            action = CourseSelectionService.ACTION_STOP
        }
        context.startService(intent)
    }
}
