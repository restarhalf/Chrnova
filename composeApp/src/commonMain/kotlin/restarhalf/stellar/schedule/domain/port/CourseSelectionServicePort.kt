package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.JwxtSelectionCourse
import kotlinx.coroutines.flow.StateFlow

interface CourseSelectionServicePort {

    val isSupported: Boolean

    val running: StateFlow<Boolean>

    val latestLog: StateFlow<ServiceLogEntry>

    fun start(config: SnatchServiceConfig): Boolean

    fun stop()
}

data class SnatchServiceConfig(
    val rotationId: String,
    val sessionTime: String,
    val extraRules: Map<String, String>,
    val targets: List<SnatchTarget>,
    val intervalMs: Long,
    val maxAttempts: Int,
    val refreshSessionOnAuthError: Boolean,
)

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
