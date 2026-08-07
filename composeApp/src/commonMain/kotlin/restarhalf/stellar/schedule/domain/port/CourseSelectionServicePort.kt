package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse
import kotlinx.coroutines.flow.StateFlow

/**
 * 后台抢课服务端口。
 *
 * - Android：通过 Foreground Service 实现真正后台运行
 * - iOS：受系统限制，[isSupported] 返回 false，所有方法 no-op
 */
interface CourseSelectionServicePort {

    /** 当前平台是否支持后台抢课 */
    val isSupported: Boolean

    /** 后台抢课服务运行状态（true=运行中） */
    val running: StateFlow<Boolean>

    /** 最近一条后台日志（用于 UI 同步显示，含 level 用于着色） */
    val latestLog: StateFlow<ServiceLogEntry>

    /**
     * 启动后台抢课服务。
     *
     * 调用方应先确保已 [restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase.initSession]
     * 完成，把得到的 ctx 与目标列表传入。
     *
     * @return true 启动成功；false 平台不支持或参数无效
     */
    fun start(config: SnatchServiceConfig): Boolean

    /** 停止后台抢课服务 */
    fun stop()
}

/** 后台抢课所需的全部上下文（可序列化传递给 Service） */
data class SnatchServiceConfig(
    val rotationId: String,
    val sessionTime: String,
    val extraRules: Map<String, String>,
    val targets: List<SnatchTarget>,
    val intervalMs: Long,
    val maxAttempts: Int,
    val refreshSessionOnAuthError: Boolean,
)

/** 后台服务日志条目（跨平台传递，level 用 Int 避免 port 层依赖 UI 枚举） */
data class ServiceLogEntry(
    val message: String,
    val level: Int = LEVEL_INFO,
) {
    companion object {
        const val LEVEL_INFO = 0
        const val LEVEL_SUCCESS = 1
        const val LEVEL_WARN = 2
        const val LEVEL_ERROR = 3
    }
}

/** 抢课目标（仅保留 Service 所需的最小字段，避免直接依赖远端 DTO） */
data class SnatchTarget(
    val key: String,
    val classificationCode: String,
    val courseId: String,
    val noticeId: String,
    val splitIdentification: String,
    val courseName: String,
    val kxh: String,
    val classTeacher: String,
) {
    companion object {
        fun from(course: JwxtSelectionCourse, classificationCode: String): SnatchTarget =
            SnatchTarget(
                key = "${course.courseId}|${course.noticeId}|${course.kxh}",
                classificationCode = classificationCode,
                courseId = course.courseId,
                noticeId = course.noticeId,
                splitIdentification = course.splitIdentification,
                courseName = course.courseName,
                kxh = course.kxh,
                classTeacher = course.classTeacher,
            )
    }
}
