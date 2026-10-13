package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtSelectedCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionClassification
import restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionOperResult
import restarhalf.stellar.schedule.data.remote.JwxtSelectionResponse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import kotlin.runCatching


class CourseSelectionUseCase(
    private val gateway: JwxtGateway,
    private val authWorkflow: JwxtAuthWorkflowPort,
) {

    
    data class SessionContext(
        val rotationId: String,
        val sessionTime: String,
        val classifications: List<JwxtSelectionClassification>,
        
        val extraRules: Map<String, String>,
    )

    private val json = Json { ignoreUnknownKeys = true }

    
    suspend fun loadRotations(): List<JwxtSelectionRotation> {
        authWorkflow.ensureLoggedIn()
        val resp = gateway.fetchSelectionRotations(isnew = 1)
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.resolvedMessage().ifBlank { "获取选课轮次失败" })
        }
        return parseList(resp.data, JwxtSelectionRotation.serializer(), "选课轮次列表")
    }

    
    suspend fun initSession(rotationId: String): SessionContext {
        authWorkflow.ensureLoggedIn()
        val resp = gateway.initSelectionSession(rotationId)
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.resolvedMessage().ifBlank { "进入选课失败" })
        }
        return parseSessionContext(rotationId, resp.data)
    }

    
    suspend fun loadCourses(
        ctx: SessionContext,
        classificationCode: String,
        courseInformation: String = "",
    ): List<JwxtSelectionCourse> {
        val resp = gateway.fetchSelectionCourses(
            rotationId = ctx.rotationId,
            classificationCode = classificationCode,
            sessionTime = ctx.sessionTime,
            extraRules = ctx.extraRules,
            courseInformation = courseInformation,
        )
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.resolvedMessage().ifBlank { "获取课程列表失败" })
        }
        return parseList(resp.data, JwxtSelectionCourse.serializer(), "课程列表")
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

    
    suspend fun drop(ctx: SessionContext, noticeId: String): JwxtSelectionResponse {
        return gateway.dropSelection(
            rotationId = ctx.rotationId,
            noticeId = noticeId,
            sessionTime = ctx.sessionTime,
            courseQualification = ctx.extraRules["courseQualification"] ?: "true",
        )
    }

    
    suspend fun loadSelectedCourses(ctx: SessionContext): List<JwxtSelectedCourse> {
        val resp = gateway.fetchSelectedCourses(rotationId = ctx.rotationId)
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.resolvedMessage().ifBlank { "获取已选课程失败" })
        }
        return parseList(resp.data, JwxtSelectedCourse.serializer(), "已选课程列表")
    }

    
    suspend fun refreshSession(rotationId: String): SessionContext {
        authWorkflow.refreshSession()
        return initSession(rotationId)
    }

    
    private fun <T> parseList(
        data: JsonElement?,
        serializer: kotlinx.serialization.KSerializer<T>,
        label: String,
    ): List<T> {
        if (data == null) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(serializer), data.toString())
        }.getOrElse {
            AppLogger.log("CourseSelection", "解析$label 失败", it)
            emptyList()
        }
    }

    
    private fun parseSessionContext(
        rotationId: String,
        data: JsonElement?,
    ): SessionContext {
        val obj = data as? JsonObject
            ?: throw IllegalStateException("会话初始化失败：响应格式异常")
        val sessionTime = obj["sessionTime"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (sessionTime.isBlank()) {
            throw IllegalStateException("会话初始化失败：未获取到 sessionTime")
        }
        val classifications = (obj["classificationList"] as? JsonArray)?.let { arr ->
            runCatching {
                json.decodeFromString(
                    ListSerializer(JwxtSelectionClassification.serializer()),
                    arr.toString(),
                )
            }.getOrElse {
                AppLogger.log("CourseSelection", "解析选课分类列表失败", it)
                emptyList()
            }
        } ?: emptyList()

        val extraRules = buildMap {
            BOOL_RULE_KEYS.forEach { key ->
                val v = obj[key]
                val s = (v as? JsonPrimitive)?.contentOrNull
                if (s == "true" || s == "false") put(key, s)
            }
            val cq = obj["courseQualification"]?.jsonPrimitive?.contentOrNull
            if (!cq.isNullOrBlank()) put("courseQualification", cq)
        }

        return SessionContext(
            rotationId = rotationId,
            sessionTime = sessionTime,
            classifications = classifications,
            extraRules = extraRules,
        )
    }

    private companion object {
        
        const val MAX_NEEDCF_DEPTH = 3

        
        val BOOL_RULE_KEYS = listOf(
            "compulsorySemester",
            "compulsorySelection",
            "compulsoryGrades",
            "selectionGrades",
            "departmentCurriculum",
        )
    }
}
