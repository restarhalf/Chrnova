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
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import kotlin.runCatching

/**
 * 选课抢课用例
 *
 * 封装教务系统选课相关单次操作：加载轮次、初始化会话、加载课程、提交选课、退课。
 * 抢课的循环重试逻辑由 [restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel] 管理，
 * 本用例只负责每次操作的请求与解析。
 *
 * @param gateway 教务系统网关
 * @param authWorkflow 认证工作流，用于确保已登录
 */
class CourseSelectionUseCase(
    private val gateway: JwxtGateway,
    private val authWorkflow: AuthWorkflowPort,
) {

    /** 初始化会话后返回的上下文，后续选课/退课都需要带上 */
    data class SessionContext(
        val rotationId: String,
        val sessionTime: String,
        val classifications: List<JwxtSelectionClassification>,
        /** 选课规则位，原样回传给选课/退课接口 */
        val extraRules: Map<String, String>,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** 加载选课轮次列表 */
    suspend fun loadRotations(): List<JwxtSelectionRotation> {
        authWorkflow.ensureLoggedIn()
        val resp = gateway.fetchSelectionRotations(isnew = 1)
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.resolvedMessage().ifBlank { "获取选课轮次失败" })
        }
        return parseList(resp.data, JwxtSelectionRotation.serializer(), "选课轮次列表")
    }

    /** 进入选课轮次并初始化会话 */
    suspend fun initSession(rotationId: String): SessionContext {
        authWorkflow.ensureLoggedIn()
        val resp = gateway.initSelectionSession(rotationId)
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.resolvedMessage().ifBlank { "进入选课失败" })
        }
        return parseSessionContext(rotationId, resp.data)
    }

    /**
     * 加载指定分类下的可选课程列表。
     *
     * @param courseInformation 课程名称搜索关键词（原样回传给 wxgetKcList 的 courseInformation 参数，空串不筛选）
     */
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

    /**
     * 提交一次选课。
     *
     * 对于 success_needcf（需选关联教学班）的响应，自动尝试选关联课程列表中
     * noticeId == yxjx0404id 的教学班，最多重试 [MAX_NEEDCF_DEPTH] 次。
     *
     * @return 最终的选课结果
     */
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

            // 重新拉取课程列表，找到与 yxjx0404id 匹配的教学班
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

            // 携带前一次已选的教学班作为 selectedNoticeId
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

    /** 退课 */
    suspend fun drop(ctx: SessionContext, noticeId: String): JwxtSelectionResponse {
        return gateway.dropSelection(
            rotationId = ctx.rotationId,
            noticeId = noticeId,
            sessionTime = ctx.sessionTime,
            courseQualification = ctx.extraRules["courseQualification"] ?: "true",
        )
    }

    /** 加载已选课程列表（用于退课，isCanTk=1 表示可退课） */
    suspend fun loadSelectedCourses(ctx: SessionContext): List<JwxtSelectedCourse> {
        val resp = gateway.fetchSelectedCourses(rotationId = ctx.rotationId)
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.resolvedMessage().ifBlank { "获取已选课程失败" })
        }
        return parseList(resp.data, JwxtSelectedCourse.serializer(), "已选课程列表")
    }

    /** 刷新会话（token 过期或 sessionTime 失效时调用） */
    suspend fun refreshSession(rotationId: String): SessionContext {
        authWorkflow.refreshSession()
        return initSession(rotationId)
    }

    /** 解析 JSON 数组元素为列表 */
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

    /** 从 wxinitXscache 响应中解析 sessionTime、分类列表与规则位 */
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
        /** needcf 关联教学班自动选择的最大深度，防止无限循环 */
        const val MAX_NEEDCF_DEPTH = 3

        /** wxinitXscache 返回的布尔规则位字段名，回传给后续接口 */
        val BOOL_RULE_KEYS = listOf(
            "compulsorySemester",
            "compulsorySelection",
            "compulsoryGrades",
            "selectionGrades",
            "departmentCurriculum",
        )
    }
}
