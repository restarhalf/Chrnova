package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.JwxtSelectedCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionClassification
import restarhalf.stellar.schedule.domain.model.JwxtSelectionCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionOperResult
import restarhalf.stellar.schedule.domain.model.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.port.CourseSelectionPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.JwxtSelectionDropResult
import kotlin.runCatching

class CourseSelectionUseCase(
    private val gateway: CourseSelectionPort,
    private val authWorkflow: JwxtAuthWorkflowPort,
) {

    data class SessionContext(
        val rotationId: String,
        val sessionTime: String,
        val classifications: List<JwxtSelectionClassification>,

        val extraRules: Map<String, String>,
    )

    suspend fun loadRotations(): List<JwxtSelectionRotation> {
        authWorkflow.ensureLoggedIn()
        return gateway.fetchRotations()
    }

    suspend fun initSession(rotationId: String): SessionContext {
        authWorkflow.ensureLoggedIn()
        val session = gateway.initSession(rotationId)
        return SessionContext(
            rotationId = rotationId,
            sessionTime = session.sessionTime,
            classifications = session.classifications,
            extraRules = session.extraRules,
        )
    }

    suspend fun loadCourses(
        ctx: SessionContext,
        classificationCode: String,
        courseInformation: String = "",
    ): List<JwxtSelectionCourse> {
        return gateway.fetchCourses(
            rotationId = ctx.rotationId,
            classificationCode = classificationCode,
            sessionTime = ctx.sessionTime,
            extraRules = ctx.extraRules,
            courseInformation = courseInformation,
        )
    }

    suspend fun submitOnce(
        ctx: SessionContext,
        classificationCode: String,
        course: JwxtSelectionCourse,
    ): JwxtSelectionOperResult {
        var currentCourse = course
        var result = gateway.submitSelection(
            rotationId = ctx.rotationId,
            courseId = currentCourse.courseId,
            noticeId = currentCourse.noticeId,
            sessionTime = ctx.sessionTime,
            classificationCode = classificationCode,
            splitIdentification = currentCourse.splitIdentification,
            selectedNoticeId = "",
            selectedSplitIdentification = "",
            extraRules = ctx.extraRules,
        )

        var depth = 0
        while (result is JwxtSelectionOperResult.NeedConfirm && depth < MAX_NEEDCF_DEPTH) {
            depth++
            val relatedNoticeId = result.yxjx0404id
            AppLogger.log(
                "CourseSelection",
                "needcf: yxcfbs=${result.yxcfbs}, cfbs=${result.cfbs}, xkkcid=${result.xkkcid}, yxjx0404id=$relatedNoticeId, msg=${result.message}",
            )

            val courses = runCatching {
                loadCourses(ctx, classificationCode)
            }.getOrElse { e ->
                if (e is CancellationException) throw e
                AppLogger.log("CourseSelection", "needcf: 重新加载课程列表失败", e)
                emptyList()
            }
            val next = courses.firstOrNull { it.noticeId == relatedNoticeId }
            if (next == null) {
                AppLogger.log(
                    "CourseSelection",
                    "needcf: 未在课程列表中找到关联教学班 noticeId=$relatedNoticeId，放弃自动选关联课",
                )
                return result
            }

            val prevNoticeId = currentCourse.noticeId
            val prevSplit = currentCourse.splitIdentification
            result = gateway.submitSelection(
                rotationId = ctx.rotationId,
                courseId = next.courseId,
                noticeId = next.noticeId,
                sessionTime = ctx.sessionTime,
                classificationCode = classificationCode,
                splitIdentification = next.splitIdentification,
                selectedNoticeId = prevNoticeId,
                selectedSplitIdentification = prevSplit,
                extraRules = ctx.extraRules,
            )
            currentCourse = next
        }
        return result
    }

    suspend fun drop(ctx: SessionContext, noticeId: String): JwxtSelectionDropResult {
        return gateway.dropSelection(
            rotationId = ctx.rotationId,
            noticeId = noticeId,
            sessionTime = ctx.sessionTime,
            courseQualification = ctx.extraRules["courseQualification"] ?: "true",
        )
    }

    suspend fun loadSelectedCourses(ctx: SessionContext): List<JwxtSelectedCourse> {
        return gateway.fetchSelectedCourses(rotationId = ctx.rotationId)
    }

    suspend fun refreshSession(rotationId: String): SessionContext {
        authWorkflow.refreshSession()
        return initSession(rotationId)
    }

    private companion object {

        const val MAX_NEEDCF_DEPTH = 3
    }
}
