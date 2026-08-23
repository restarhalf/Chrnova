package restarhalf.stellar.schedule

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.context.GlobalContext
import restarhalf.stellar.schedule.CourseSelectionService.Companion.latestLogFlow
import restarhalf.stellar.schedule.CourseSelectionService.Companion.pendingConfig
import restarhalf.stellar.schedule.CourseSelectionService.Companion.runningFlow
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionOperResult
import restarhalf.stellar.schedule.domain.port.ServiceLogEntry
import restarhalf.stellar.schedule.domain.port.SnatchServiceConfig
import restarhalf.stellar.schedule.domain.port.SnatchTarget
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * 自动抢课 Foreground Service。
 *
 * - 通过 [pendingConfig] 静态字段接收启动参数（避免 Intent 大小限制）
 * - 通过 [runningFlow] / [latestLogFlow] 向 UI 同步状态
 * - 内部使用 Koin 全局上下文获取 [CourseSelectionUseCase]
 */
class CourseSelectionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var snatchJob: Job? = null
    private var config: SnatchServiceConfig? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSnatch()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val cfg = pendingConfig
                if (cfg == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                pendingConfig = null
                config = cfg
                startForeground(NOTIFICATION_ID, buildNotification("自动抢课运行中", "目标数：${cfg.targets.size}"))
                startSnatch(cfg)
            }
        }
        return START_STICKY
    }

    private fun startSnatch(cfg: SnatchServiceConfig) {
        if (snatchJob?.isActive == true) return
        runningFlow.value = true
        emitLog("后台抢课已启动，目标数：${cfg.targets.size}", ServiceLogEntry.LEVEL_INFO)
        snatchJob = scope.launch {
            runSnatchLoop(cfg)
        }
    }

    private fun stopSnatch() {
        snatchJob?.cancel()
        snatchJob = null
        runningFlow.value = false
        emitLog("后台抢课已停止", ServiceLogEntry.LEVEL_WARN)
    }

    /** 抢课主循环：复用 ViewModel 同样的循环结构 */
    private suspend fun runSnatchLoop(cfg: SnatchServiceConfig) {
        val koin = GlobalContext.getOrNull() ?: run {
            emitLog("Koin 未初始化，停止", ServiceLogEntry.LEVEL_ERROR)
            stopSelf()
            return
        }
        val useCase = koin.get<CourseSelectionUseCase>()
        var ctx = CourseSelectionUseCase.SessionContext(
            rotationId = cfg.rotationId,
            sessionTime = cfg.sessionTime,
            classifications = emptyList(),
            extraRules = cfg.extraRules,
        )
        // 标记各目标状态
        val pending = cfg.targets.toMutableList()
        var attempt = 0

        try {
            while (scope.isActive) {
                attempt++
                if (cfg.maxAttempts in 1..<attempt) {
                    emitLog("已达最大尝试次数 ${cfg.maxAttempts}，停止", ServiceLogEntry.LEVEL_WARN)
                    break
                }
                if (pending.isEmpty()) {
                    emitLog("所有目标已选课成功", ServiceLogEntry.LEVEL_SUCCESS)
                    break
                }
                val iter = pending.iterator()
                while (iter.hasNext() && scope.isActive) {
                    val target = iter.next()
                    try {
                        val course = target.toCourse()
                        val result = useCase.submitOnce(ctx, target.classificationCode, course)
                        when (result) {
                            is JwxtSelectionOperResult.Success -> {
                                emitLog("${target.courseName}：${result.message}", ServiceLogEntry.LEVEL_SUCCESS)
                                updateNotification("已选上：${target.courseName}", "剩余 ${pending.size - 1} 个目标")
                                iter.remove()
                            }
                            is JwxtSelectionOperResult.NeedConfirm -> {
                                emitLog("${target.courseName}：需确认 - ${result.message}", ServiceLogEntry.LEVEL_WARN)
                            }
                            is JwxtSelectionOperResult.Fail -> {
                                emitLog("${target.courseName}：${result.message}", ServiceLogEntry.LEVEL_ERROR)
                            }
                            is JwxtSelectionOperResult.Unknown -> {
                                emitLog("${target.courseName}：${result.message}", ServiceLogEntry.LEVEL_WARN)
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        AppLogger.log("CourseSelectionService", "抢课请求异常", e)
                        emitLog("${target.courseName}：${e.message ?: "请求异常"}", ServiceLogEntry.LEVEL_ERROR)

                        // 认证失效则刷新会话
                        if (cfg.refreshSessionOnAuthError && isAuthError(e)) {
                            emitLog("认证失效，刷新会话...", ServiceLogEntry.LEVEL_WARN)
                            try {
                                ctx = useCase.refreshSession(ctx.rotationId)
                            } catch (refreshErr: Exception) {
                                if (refreshErr is CancellationException) throw refreshErr
                                emitLog("刷新会话失败：${refreshErr.message}", ServiceLogEntry.LEVEL_ERROR)
                                break
                            }
                        }
                    }
                }
                try {
                    delay(cfg.intervalMs.milliseconds)
                } catch (e: CancellationException) {
                    throw e
                }
            }
        } catch (e: CancellationException) {
            emitLog("抢课被取消", ServiceLogEntry.LEVEL_WARN)
            throw e
        } finally {
            runningFlow.value = false
            emitLog("抢课循环结束（共 $attempt 次）", ServiceLogEntry.LEVEL_INFO)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun isAuthError(e: Throwable): Boolean {
        val msg = e.message.orEmpty()
        return msg.contains("登录") || msg.contains("认证") || msg.contains("token") ||
            msg.contains("401") || msg.contains("session")
    }

    private fun SnatchTarget.toCourse(): JwxtSelectionCourse = JwxtSelectionCourse(
        courseName = courseName,
        courseId = courseId,
        noticeId = noticeId,
        kxh = kxh,
        classTeacher = classTeacher,
        splitIdentification = splitIdentification,
    )

    private fun emitLog(msg: String, level: Int = ServiceLogEntry.LEVEL_INFO) {
        val ts = now()
        val line = "[$ts] $msg"
        latestLogFlow.value = ServiceLogEntry(message = line, level = level)
        AppLogger.log("CourseSelectionService", msg)
    }

    private fun now(): String {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val dt = Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.hour.toString().padStart(2, '0')}:" +
            "${dt.minute.toString().padStart(2, '0')}:" +
                dt.second.toString().padStart(2, '0')
    }

    private fun buildNotification(title: String, content: String): Notification {
        ensureChannel()
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pi = launchIntent?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun ensureChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "自动抢课",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "后台抢课服务运行状态通知"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        snatchJob?.cancel()
        scope.cancel()
        runningFlow.value = false
    }

    companion object {
        const val ACTION_START = "restarhalf.stellar.schedule.action.START_SNATCH"
        const val ACTION_STOP = "restarhalf.stellar.schedule.action.STOP_SNATCH"
        const val CHANNEL_ID = "course_selection_snatch"
        const val NOTIFICATION_ID = 0xC0EE

        /** 由 Port 写入、Service 读取的启动配置 */
        @Volatile
        var pendingConfig: SnatchServiceConfig? = null

        /** 后台抢课运行状态（跨进程共享，由 Service 单实例维护） */
        val runningFlow = MutableStateFlow(false)

        /** 最近一条后台日志（带 level） */
        val latestLogFlow = MutableStateFlow(ServiceLogEntry(""))
    }
}
