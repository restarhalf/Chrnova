package restarhalf.stellar.schedule.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import restarhalf.stellar.schedule.domain.port.PEPasswordEncryptionPort
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 体育系统HTTP客户端
 *
 * 注意：本类中URL为第三方体育系统地址（39.100.89.70），该服务器仅支持HTTP，无法升级为HTTPS。
 */
class PEClient(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val authStore: PEAuthStore,
    private val passwordEncryption: PEPasswordEncryptionPort,
) : PEGateway {
    private val baseUrl = "http://39.100.89.70/service"

    /**
     * 通用认证请求模板：取 token → 构建请求 → 发送 → 校验 → 返回响应体。
     *
     * 调用方负责解析响应体并检查 status 字段。
     *
     * @param url 请求地址
     * @param requestBody 请求体 JSON
     * @return 原始响应体字符串
     */
    private suspend fun executeWithAuth(
        url: String,
        requestBody: kotlinx.serialization.json.JsonElement,
    ): String = withContext(AppIoDispatcher) {
        val token = authStore.getToken() ?: throw PETokenExpiredException("请先登录体测系统")

        val response: HttpResponse = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            header("Referer", "$baseUrl/mobile/")
            setBody(requestBody)
        }

        // 原生端通过响应头 abnormal 区分：NOFUN=无权限，TIMEOUT=登录超时
        when (response.headers["abnormal"]?.uppercase()) {
            "NOFUN" -> throw IllegalStateException("您没有操作权限")
            "TIMEOUT" -> throw PETokenExpiredException("登录超时，请重新登录")
        }

        if (response.status.value == 401) throw PETokenExpiredException("登录已过期，请重新登录")
        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                response.extractServerErrorMessage(json, listOf("message", "msg"))
                    ?: "请求失败（HTTP ${response.status.value}）"
            )
        }

        response.bodyAsText()
    }

    /**
     * 解析响应体并校验 status == "PASS"。
     * FAIL 时：像登录/令牌类问题抛 PETokenExpiredException；业务失败（如无权限）抛带服务端 message 的异常。
     */
    private inline fun <reified T> parseAndVerify(
        body: String,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): T {
        val parsed = json.decodeFromString(deserializer, body)
        val (status, message) = when (parsed) {
            is PELoginResponse -> parsed.status to parsed.message
            is PEScoreListResponse -> parsed.status to parsed.message
            is PEDetailResponse -> parsed.status to parsed.message
            is PESubjectHistoryResponse -> parsed.status to parsed.message
            is PEAuthProfileResponse -> parsed.status to parsed.message
            is PEAppointmentListResponse -> parsed.status to parsed.message
            is PEAppointmentDetailResponse -> parsed.status to parsed.message
            is PEAppointmentTimesResponse -> parsed.status to parsed.message
            is PEAppointmentActionResponse -> parsed.status to parsed.message
            is PEFreeApplyListResponse -> parsed.status to parsed.message
            is PEFreeSchoolYearResponse -> parsed.status to parsed.message
            is PEFreeActionResponse -> parsed.status to parsed.message
            else -> "PASS" to ""
        }
        if (status != "PASS") {
            throw peFailException(message)
        }
        return parsed
    }

    /**
     * 将 PE 业务 FAIL 转为异常。
     * 仅明确的登录/会话问题才抛 PETokenExpiredException；无权限、业务失败原样透出。
     */
    private fun peFailException(message: String): Exception {
        val msg = message.trim()
        val tokenLike = listOf("登录超时", "登录已过期", "会话过期", "重新登录", "未登录", "token失效", "令牌")
            .any { msg.contains(it, ignoreCase = true) }
        // 权限类优先，避免被登录态逻辑吞掉
        val permissionLike = listOf("权限", "无权", "未授权访问", "NOFUN")
            .any { msg.contains(it, ignoreCase = true) }
        return when {
            permissionLike -> IllegalStateException(msg.ifBlank { "您没有操作权限" })
            tokenLike -> PETokenExpiredException(msg)
            msg.isBlank() -> IllegalStateException("操作失败")
            else -> IllegalStateException(msg)
        }
    }

    override suspend fun login(username: String, password: String): PELoginResponse =
        withContext(AppIoDispatcher) {
            val encryptedPassword = passwordEncryption.encryptPasswordForPELogin(password)
            val sign = passwordEncryption.generatePESign(
                mapOf(
                    "username" to username,
                    "password" to encryptedPassword,
                    "sys_id" to "iscpMobile"
                )
            )

            val requestBody = buildJsonObject {
                put("username", username)
                put("password", encryptedPassword)
                put("sys_id", "iscpMobile")
                put("nonceStr", "")
                put("captchaValue", "")
                put("sign", sign)
            }

            val response: HttpResponse = httpClient.post("$baseUrl/login/mobile/check") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (!response.status.isSuccess()) {
                throw IllegalStateException(
                    response.extractServerErrorMessage(json, listOf("message", "msg"))
                        ?: "登录失败（HTTP ${response.status.value}）"
                )
            }

            val body = response.bodyAsText()
            if (body.isBlank()) {
                throw PETokenExpiredException()
            }

            val parsed = json.decodeFromString(PELoginResponse.serializer(), body)
            if (parsed.status == "PASS") {
                parsed.token?.let { authStore.setToken(it) }
                parsed.userId?.let { authStore.setUserId(it) }
            }
            parsed
        }

    override suspend fun getScoreList(): PEScoreListResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException()
        val sign = passwordEncryption.generatePESign(mapOf("user_id" to userId))
        val requestBody = buildJsonObject {
            put("user_id", userId)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/gymResult/selectUserPlanList",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PEScoreListResponse.serializer())
    }

    override suspend fun getScoreDetail(schoolYear: String): PEDetailResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException()
        val sign = passwordEncryption.generatePESign(
            mapOf("user_id" to userId, "school_year" to schoolYear)
        )
        val requestBody = buildJsonObject {
            put("user_id", userId)
            put("school_year", schoolYear)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/gymResult/selectUserPlanScore",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PEDetailResponse.serializer())
    }

    override suspend fun getSubjectScoreHistory(
        schoolYear: String,
        subjectId: String,
        pageNum: Int,
        pageSize: Int,
    ): PESubjectHistoryResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException()
        val sign = passwordEncryption.generatePESign(
            mapOf(
                "user_id" to userId,
                "school_year" to schoolYear,
                "subject_id" to subjectId,
                "page_num" to pageNum,
                "page_size" to pageSize,
            )
        )
        val requestBody = buildJsonObject {
            put("user_id", userId)
            put("school_year", schoolYear)
            put("subject_id", subjectId)
            put("page_num", pageNum)
            put("page_size", pageSize)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/gymResult/selectUserPlanSubjectScore",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PESubjectHistoryResponse.serializer())
    }

    override suspend fun getProfile(): PEAuthProfileResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException()
        val sign = passwordEncryption.generatePESign(mapOf("userId" to userId))
        val requestBody = buildJsonObject {
            put("userId", userId)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/sysUser/mobile/findStudent",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PEAuthProfileResponse.serializer())
    }

    override suspend fun getAppointments(
        type: String,
        pageNum: Int,
        pageSize: Int,
    ): PEAppointmentListResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException()
        val params = mapOf(
            "user_id" to userId,
            "type" to type,
            "page_num" to pageNum,
            "page_size" to pageSize,
        )
        val sign = passwordEncryption.generatePESign(params)
        val requestBody = buildJsonObject {
            put("user_id", userId)
            put("type", type)
            put("page_num", pageNum)
            put("page_size", pageSize)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/appointment/selectUserAppointmentList",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PEAppointmentListResponse.serializer())
    }

    override suspend fun getAppointmentDetail(
        appointmentId: String,
        appointmentStatus: String,
    ): PEAppointmentDetailResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException()
        val params = mapOf(
            "appointment_id" to appointmentId,
            "appointment_status" to appointmentStatus,
            "user_id" to userId,
        )
        val sign = passwordEncryption.generatePESign(params)
        val requestBody = buildJsonObject {
            put("appointment_id", appointmentId)
            put("appointment_status", appointmentStatus)
            put("user_id", userId)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/appointment/selectUserAppointmentDetail",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PEAppointmentDetailResponse.serializer())
    }

    override suspend fun getAppointmentTimes(
        appointmentId: String,
        appointmentDate: String,
    ): PEAppointmentTimesResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException()
        val params = mapOf(
            "appointment_id" to appointmentId,
            "appointment_date" to appointmentDate,
            "user_id" to userId,
        )
        val sign = passwordEncryption.generatePESign(params)
        val requestBody = buildJsonObject {
            put("appointment_id", appointmentId)
            put("appointment_date", appointmentDate)
            put("user_id", userId)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/appointment/selectAppointmentTimes",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PEAppointmentTimesResponse.serializer())
    }

    override suspend fun cancelAppointment(temporaryId: String): PEAppointmentActionResponse {
        val sign = passwordEncryption.generatePESign(mapOf("temporary_id" to temporaryId))
        val requestBody = buildJsonObject {
            put("temporary_id", temporaryId)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/appointment/cancelRegistration",
            requestBody = requestBody,
        )
        return parseActionOrThrow(body)
    }

    override suspend fun enterAppointment(
        appointmentId: String,
        timesId: String,
        enterDate: String,
    ): PEAppointmentActionResponse {
        val params = mapOf(
            "appointment_id" to appointmentId,
            "times_id" to timesId,
            "enter_date" to enterDate,
        )
        val sign = passwordEncryption.generatePESign(params)
        val requestBody = buildJsonObject {
            put("appointment_id", appointmentId)
            put("times_id", timesId)
            put("enter_date", enterDate)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/appointment/appointmentEnter",
            requestBody = requestBody,
        )
        return parseActionOrThrow(body)
    }

    /**
     * 解析预约操作响应；业务失败时抛出带服务端 message 的异常，避免被当作令牌过期。
     */
    private fun parseActionOrThrow(body: String): PEAppointmentActionResponse {
        val parsed = json.decodeFromString(PEAppointmentActionResponse.serializer(), body)
        if (parsed.status != "PASS") {
            throw peFailException(parsed.message)
        }
        return parsed
    }

    override suspend fun getFreeApplyList(): PEFreeApplyListResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException("请先登录体测系统")
        val sign = passwordEncryption.generatePESign(mapOf("userId" to userId))
        val requestBody = buildJsonObject {
            put("userId", userId)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/gymFreeManager/selectList",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PEFreeApplyListResponse.serializer())
    }

    override suspend fun getFreeSchoolYears(): PEFreeSchoolYearResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException("请先登录体测系统")
        val sign = passwordEncryption.generatePESign(mapOf("userId" to userId))
        val requestBody = buildJsonObject {
            put("userId", userId)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/gymFreeManager/selectFreeSchoolYear",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PEFreeSchoolYearResponse.serializer())
    }

    /**
     * 提交免测申请。
     */
    override suspend fun submitFreeApply(
        stdNumber: String,
        schoolYear: String,
        freeApplyType: String,
        attachments: List<String>,
    ): PEFreeActionResponse {
        val userId = authStore.getUserId() ?: throw PETokenExpiredException("请先登录体测系统")
        val attIds = attachments.filter { it.isNotBlank() }
        if (attIds.isEmpty()) throw IllegalStateException("请上传附件")
        val attachmentsParam = attIds.joinToString(separator = ",", postfix = ",")
        val params = mapOf(
            "stdNumber" to stdNumber,
            "userId" to userId,
            "attachments" to attachmentsParam,
            "schoolYear" to schoolYear,
            "freeApplyType" to freeApplyType,
        )
        val sign = passwordEncryption.generatePESign(params)
        val requestBody = buildJsonObject {
            put("stdNumber", stdNumber)
            put("userId", userId)
            put("attachments", attachmentsParam)
            put("schoolYear", schoolYear)
            put("freeApplyType", freeApplyType)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/gymFreeManager/studentApply",
            requestBody = requestBody,
        )
        return parseFreeActionOrThrow(body)
    }

    /** 上传附件。抓包确认 sign 为 generatePESign({fileTime})，与 JSON 接口同一 SHA1 规则。 */
    override suspend fun uploadPeFile(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): PEFreeActionResponse = withContext(AppIoDispatcher) {
        if (bytes.isEmpty()) throw IllegalStateException("文件内容为空")
        if (bytes.size > 5 * 1024 * 1024) throw IllegalStateException("每个文件上传大小不能超过5M")
        val token = authStore.getToken() ?: throw PETokenExpiredException("请先登录体测系统")
        // 原站 fileTime 为 JS Date.toString()；此处用稳定字符串，签名与表单字段保持一致即可
        val fileTime = "file-${kotlin.time.Clock.System.now().toEpochMilliseconds()}"
        val sign = passwordEncryption.generatePESign(mapOf("fileTime" to fileTime))

        val response: HttpResponse = httpClient.post("$baseUrl/common/mobile/saveFile") {
            header("Authorization", token)
            header("Referer", "$baseUrl/mobile/")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, mimeType.ifBlank { "application/octet-stream" })
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            },
                        )
                        append("fileTime", fileTime)
                        append("sign", sign)
                    },
                ),
            )
        }

        when (response.headers["abnormal"]?.uppercase()) {
            "NOFUN" -> throw IllegalStateException("您没有操作权限")
            "TIMEOUT" -> throw PETokenExpiredException("登录超时，请重新登录")
        }
        if (response.status.value == 401) throw PETokenExpiredException("登录已过期，请重新登录")
        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                response.extractServerErrorMessage(json, listOf("message", "msg"))
                    ?: "上传失败（HTTP ${response.status.value}）",
            )
        }
        val parsed = json.decodeFromString(
            PEFreeActionResponse.serializer(),
            response.bodyAsText(),
        )
        if (parsed.status != "PASS" || parsed.attId.isNullOrBlank()) {
            throw peFailException(parsed.message.ifBlank { "上传失败" })
        }
        parsed
    }

    private fun parseFreeActionOrThrow(body: String): PEFreeActionResponse {
        val parsed = json.decodeFromString(PEFreeActionResponse.serializer(), body)
        if (parsed.status != "PASS") {
            throw peFailException(parsed.message)
        }
        return parsed
    }
}
