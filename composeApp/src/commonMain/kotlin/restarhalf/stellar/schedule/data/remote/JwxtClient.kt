package restarhalf.stellar.schedule.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.PasswordEncryptionPort
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 教务系统HTTP客户端
 *
 * 注意：本类中所有URL均为第三方教务系统地址（jwyd.dlnu.edu.cn），该服务器仅支持HTTP，无法升级为HTTPS。
 */
class JwxtClient(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val authStore: JwxtAuthStore? = null,
    private val passwordEncryption: PasswordEncryptionPort,
) : JwxtGateway {

    private companion object {
        const val BASE_URL = "http://jwyd.dlnu.edu.cn/njwhd"
        const val SELECTION_BASE_URL = "http://jwyd.dlnu.edu.cn/jsxsd/qzapp"
    }

    private suspend fun executePostEmpty(url: String): String =
        withContext(AppIoDispatcher) {
            val response: HttpResponse = httpClient.post(url)
            if (!response.status.isSuccess()) {
                throw IllegalStateException(
                    response.extractServerErrorMessage()
                        ?: "请求失败（HTTP ${response.status.value}）"
                )
            }
            response.body()
        }

    private suspend fun HttpResponse.extractServerErrorMessage(): String? =
        extractServerErrorMessage(json, fieldPriority = listOf("msg", "Msg", "message"))

    override suspend fun login(
        userNo: String,
        password: String,
        captchaData: String,
        codeVal: String,
        p: String?
    ): JwxtLoginResponse =
        withContext(AppIoDispatcher) {
            val encryptedPassword = passwordEncryption.encryptPasswordForLogin(password)

            val parameters =
                Parameters.build {
                    append("userNo", userNo)
                    append("pwd", encryptedPassword)
                    append("captchaData", captchaData)
                    append("codeVal", codeVal)
                    if (!p.isNullOrBlank()) append("p", p)
                }

            val url = "$BASE_URL/login"

            val response: HttpResponse =
                httpClient.submitForm(url = url, formParameters = parameters)

            if (!response.status.isSuccess()) {
                throw IllegalStateException(
                    response.extractServerErrorMessage()
                        ?: "登录失败（HTTP ${response.status.value}）"
                )
            }

            val body: String = response.body()

            val serverId =
                extractServerIdFromSetCookie(response.headers.getAll("Set-Cookie") ?: emptyList())
            if (!serverId.isNullOrBlank()) {
                authStore?.setServerIdCookie(serverId)
            }

            val parsed = json.decodeFromString(JwxtLoginResponse.serializer(), body)
            if (parsed.code == 1) {
                authStore?.setToken(parsed.data?.token)
            }
            parsed
        }

    override suspend fun getCurrentTerm(): JwxtCurrentTermResponse {
        val body = executePostEmpty("$BASE_URL/currentTerm")
        return json.decodeFromString(
            JwxtApiResponse.serializer(ListSerializer(JwxtCurrentTermItem.serializer())), body
        )
    }

    override suspend fun getSemesterList(): List<JwxtSemesterItem> {
        val body = executePostEmpty("$BASE_URL/getXnxqList")
        return json.decodeFromString(ListSerializer(JwxtSemesterItem.serializer()), body)
    }

    override suspend fun getSemesterListFromEndpoint(): List<JwxtSemesterListItem> {
        val body = executePostEmpty("$BASE_URL/semesterList")
        val response = json.decodeFromString(
            JwxtApiResponse.serializer(ListSerializer(JwxtSemesterListItem.serializer())), body
        )
        if (!response.isSuccess()) {
            throw IllegalStateException(
                response.messageOrEmpty().ifBlank { "获取学期列表失败" })
        }
        return response.data ?: emptyList()
    }

    override suspend fun getTeachingWeek(): JwxtTeachingWeekResponse {
        val body = executePostEmpty("$BASE_URL/teachingWeek")
        return json.decodeFromString(JwxtTeachingWeekResponse.serializer(), body)
    }

    override suspend fun getCampusList(): JwxtCampusResponse {
        val body = executePostEmpty("$BASE_URL/Get_sjkbms")
        return json.decodeFromString(JwxtCampusResponse.serializer(), body)
    }

    override suspend fun fetchCurriculum(fields: Map<String, String>): JwxtCurriculumResponse =
        withContext(AppIoDispatcher) {
            val url = "$BASE_URL/student/curriculum"

            val response: HttpResponse =
                httpClient.post(url) {
                    fields["xnxq01id"]?.takeIf { it.isNotBlank() }
                        ?.let { parameter("xnxq01id", it) }
                    fields["kbjcmsid"]?.takeIf { it.isNotBlank() }
                        ?.let { parameter("kbjcmsid", it) }
                    fields["week"]?.takeIf { it.isNotBlank() }?.let { parameter("week", it) }
                }

            if (!response.status.isSuccess()) {
                throw IllegalStateException(
                    response.extractServerErrorMessage()
                        ?: "课表请求失败（HTTP ${response.status.value}）"
                )
            }
            response.body()
        }

    override suspend fun fetchExaminationArrangement(
        semester: String,
        nameOrNumber: String
    ): JwxtExaminationResponse =
        withContext(AppIoDispatcher) {
            val url = "$BASE_URL/student/examinationArrangement"

            val response: HttpResponse =
                httpClient.post(url) {
                    parameter("semester", semester)
                    parameter("nameOrNumber", nameOrNumber)
                }

            if (!response.status.isSuccess()) {
                throw IllegalStateException(
                    response.extractServerErrorMessage()
                        ?: "考试安排请求失败（HTTP ${response.status.value}）"
                )
            }
            response.body()
        }

    override suspend fun fetchTermGradeReport(semester: String): JwxtTermGradeResponse =
        withContext(AppIoDispatcher) {
            val url = "$BASE_URL/student/termGPA"

            val response: HttpResponse =
                httpClient.post(url) {
                    parameter("semester", semester)
                    parameter("type", "1")
                }

            if (!response.status.isSuccess()) {
                throw IllegalStateException(
                    response.extractServerErrorMessage()
                        ?: "成绩请求失败（HTTP ${response.status.value}）"
                )
            }
            json.decodeFromString(
                JwxtApiResponse.serializer(ListSerializer(JwxtTermGradeDataItem.serializer())),
                response.body<String>()
            )
        }

    override suspend fun fetchGuidanceTeachingCourses(
        kcxz: String,
        kcsx: String,
        kcmc: String
    ): JwxtGuidanceTeachingResponse =
        withContext(AppIoDispatcher) {
            val url = "$BASE_URL/student/guidanceTeaching"

            val response: HttpResponse =
                httpClient.post(url) {
                    parameter("kcxz", kcxz)
                    parameter("kcsx", kcsx)
                    parameter("kcmc", kcmc)
                }

            if (!response.status.isSuccess()) {
                throw IllegalStateException(
                    response.extractServerErrorMessage()
                        ?: "指导教学课程请求失败（HTTP ${response.status.value}）"
                )
            }
            json.decodeFromString(
                JwxtGuidanceTeachingResponse.serializer(),
                response.body<String>()
            )
        }

    private fun extractServerIdFromSetCookie(setCookies: List<String>): String? {
        for (header in setCookies) {
            val parts = header.split(';')
            val keyValue = parts.firstOrNull().orEmpty().trim()
            if (keyValue.startsWith("SERVERID=", ignoreCase = true) ||
                keyValue.startsWith("srv_id=", ignoreCase = true)
            ) {
                return keyValue
            }
        }
        return null
    }

    // ==================== 选课系统接口实现 ====================
    // 对应 http://jwyd.dlnu.edu.cn/jsxsd/qzapp/* 系列接口。
    // 这些接口均为 POST + query 参数（无 body），退课接口除外（multipart）。

    /** 通用选课 POST 请求：无 body，仅 query 参数，返回原始响应文本 */
    private suspend fun postSelectionRaw(
        endpoint: String,
        params: Map<String, String>,
    ): String =
        withContext(AppIoDispatcher) {
            val response: HttpResponse = httpClient.post("$SELECTION_BASE_URL/$endpoint") {
                params.forEach { (k, v) -> parameter(k, v) }
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException(
                    response.extractServerErrorMessage(
                        json,
                        fieldPriority = listOf("errorMessage", "msg", "Msg", "message"),
                    ) ?: "选课请求失败（HTTP ${response.status.value}）"
                )
            }
            response.body()
        }

    override suspend fun fetchSelectionRotations(isnew: Int): JwxtSelectionResponse {
        val body = postSelectionRaw("wxgetXklc", mapOf("isnew" to isnew.toString()))
        return json.decodeFromString(JwxtSelectionResponse.serializer(), body)
    }

    override suspend fun initSelectionSession(rotationId: String): JwxtSelectionResponse {
        val body = postSelectionRaw(
            "wxinitXscache",
            mapOf("rotationId" to rotationId, "appChossCurTime" to ""),
        )
        return json.decodeFromString(JwxtSelectionResponse.serializer(), body)
    }

    override suspend fun fetchSelectionCourses(
        rotationId: String,
        classificationCode: String,
        sessionTime: String,
        extraRules: Map<String, String>,
        courseInformation: String,
    ): JwxtSelectionResponse {
        val params = buildMap {
            put("classificationCode", classificationCode)
            put("rotationId", rotationId)
            put("courseId", "")
            put("noticeId", "")
            put("splitIdentification", "")
            put("courseInformation", courseInformation)
            put("classTeacher", "")
            put("classWeek", "")
            put("classSessions", "")
            put("filteringConflicts", "")
            put("restrictedSelection", "")
            put("sessionTime", sessionTime)
            put("fullCourse", "")
            put("compulsorySemester", extraRules["compulsorySemester"] ?: "true")
            put("compulsorySelection", extraRules["compulsorySelection"] ?: "true")
            put("compulsoryGrades", extraRules["compulsoryGrades"] ?: "true")
            put("selectionGrades", extraRules["selectionGrades"] ?: "true")
            put("departmentCurriculum", extraRules["departmentCurriculum"] ?: "false")
            put("generalCourseCategories", "")
            put("courseQualification", extraRules["courseQualification"] ?: "true")
            put("data_enccryptStr", "")
            put("szjylb", "")
        }
        val body = postSelectionRaw("wxgetKcList", params)
        return json.decodeFromString(JwxtSelectionResponse.serializer(), body)
    }

    override suspend fun submitSelection(
        rotationId: String,
        courseId: String,
        noticeId: String,
        sessionTime: String,
        classificationCode: String,
        splitIdentification: String,
        selectedNoticeId: String,
        selectedSplitIdentification: String,
        extraRules: Map<String, String>,
    ): JwxtSelectionOperResult {
        val params = buildMap {
            put("classificationCode", classificationCode)
            put("rotationId", rotationId)
            put("courseId", courseId)
            put("noticeId", noticeId)
            put("selectedNoticeId", selectedNoticeId)
            put("splitIdentification", splitIdentification)
            put("selectedSplitIdentification", selectedSplitIdentification)
            put("courseInformation", "")
            put("classTeacher", "")
            put("classWeek", "")
            put("classSessions", "")
            put("filteringConflicts", "")
            put("restrictedSelection", "")
            put("sessionTime", sessionTime)
            put("fullCourse", "")
            put("compulsorySemester", extraRules["compulsorySemester"] ?: "true")
            put("compulsorySelection", extraRules["compulsorySelection"] ?: "true")
            put("compulsoryGrades", extraRules["compulsoryGrades"] ?: "true")
            put("selectionGrades", extraRules["selectionGrades"] ?: "true")
            put("departmentCurriculum", extraRules["departmentCurriculum"] ?: "false")
            put("generalCourseCategories", "")
            put("courseQualification", extraRules["courseQualification"] ?: "true")
            put("data_enccryptStr", "")
            put("szjylb", "")
        }
        val body = postSelectionRaw("wxxkOper", params)
        val resp = json.decodeFromString(JwxtSelectionResponse.serializer(), body)
        return parseSelectionOperResult(resp)
    }

    override suspend fun dropSelection(
        rotationId: String,
        noticeId: String,
        sessionTime: String,
        courseQualification: String,
    ): JwxtSelectionResponse =
        withContext(AppIoDispatcher) {
            val response: HttpResponse = httpClient.post("$SELECTION_BASE_URL/wxxstkOper") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("courseQualification", courseQualification)
                            append("rotationId", rotationId)
                            append("noticeId", noticeId)
                            append("sessionTime", sessionTime)
                        },
                    ),
                )
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException(
                    response.extractServerErrorMessage(
                        json,
                        fieldPriority = listOf("errorMessage", "msg", "Msg", "message"),
                    ) ?: "退课失败（HTTP ${response.status.value}）"
                )
            }
            json.decodeFromString(JwxtSelectionResponse.serializer(), response.body())
        }

    override suspend fun fetchSelectedCourses(rotationId: String): JwxtSelectionResponse {
        val body = postSelectionRaw("wxgetYxkcList", mapOf("rotationId" to rotationId))
        return json.decodeFromString(JwxtSelectionResponse.serializer(), body)
    }

    /**
     * 解析 wxxkOper 响应的 data 字段为统一结果：
     * - success：data 可能为空字符串、关联课程对象或关联课程数组
     * - success_needcf：data 为对象，含 yxcfbs/cfbs/xkkcid/yxjx0404id
     * - fail：data 为空字符串
     */
    private fun parseSelectionOperResult(resp: JwxtSelectionResponse): JwxtSelectionOperResult {
        val msg = resp.resolvedMessage()
        val data = resp.data
        return when {
            resp.isSuccess() -> {
                val related = parseRelatedCourses(data)
                JwxtSelectionOperResult.Success(
                    message = msg.ifBlank { "选课成功" },
                    relatedCourses = related,
                )
            }

            resp.isNeedConfirm() -> {
                val obj = data as? JsonObject
                val str = { key: String -> obj?.get(key)?.jsonPrimitive?.contentOrNull.orEmpty() }
                JwxtSelectionOperResult.NeedConfirm(
                    message = msg.ifBlank { "还有关联教学班需要选" },
                    yxcfbs = str("yxcfbs"),
                    cfbs = str("cfbs"),
                    xkkcid = str("xkkcid"),
                    yxjx0404id = str("yxjx0404id"),
                )
            }

            resp.isFail() -> JwxtSelectionOperResult.Fail(msg.ifBlank { "选课失败" })

            else -> JwxtSelectionOperResult.Unknown(resp.errorCode, msg)
        }
    }

    /** 成功响应中 data 若为数组，则尝试解析为关联课程列表；对象视为单个课程；其它返回空 */
    private fun parseRelatedCourses(data: JsonElement?): List<JwxtSelectionCourse> {
        if (data == null) return emptyList()
        return when (data) {
            is JsonArray -> runCatching {
                json.decodeFromString(
                    ListSerializer(JwxtSelectionCourse.serializer()),
                    data.toString(),
                )
            }.getOrElse {
                AppLogger.log("JwxtClient", "解析关联课程数组失败", it)
                emptyList()
            }

            is JsonObject -> runCatching {
                listOf(json.decodeFromString(JwxtSelectionCourse.serializer(), data.toString()))
            }.getOrElse {
                AppLogger.log("JwxtClient", "解析关联课程对象失败", it)
                emptyList()
            }

            else -> emptyList()
        }
    }
}
