package restarhalf.stellar.schedule.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import restarhalf.stellar.schedule.domain.error.PETokenExpiredException
import restarhalf.stellar.schedule.domain.model.PEAppointmentActionResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentDetailResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentListResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentTimesResponse
import restarhalf.stellar.schedule.domain.model.PEAuthProfileResponse
import restarhalf.stellar.schedule.domain.model.PEDetailResponse
import restarhalf.stellar.schedule.domain.model.PEFreeActionResponse
import restarhalf.stellar.schedule.domain.model.PEFreeApplyDetailResponse
import restarhalf.stellar.schedule.domain.model.PEFreeApplyListResponse
import restarhalf.stellar.schedule.domain.model.PEFreeSchoolYearResponse
import restarhalf.stellar.schedule.domain.model.PELoginResponse
import restarhalf.stellar.schedule.domain.model.PEScoreListResponse
import restarhalf.stellar.schedule.domain.model.PESubjectHistoryResponse
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.port.PEPasswordEncryptionPort
import restarhalf.stellar.schedule.platform.AppIoDispatcher


class PEClient(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val authStore: PEAuthStore,
    private val passwordEncryption: PEPasswordEncryptionPort,
) : PEGateway {
    private val baseUrl = "http://39.100.89.70/service"

    
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
            is PEFreeApplyDetailResponse -> parsed.status to parsed.message
            is PEFreeSchoolYearResponse -> parsed.status to parsed.message
            is PEFreeActionResponse -> parsed.status to parsed.message
            else -> "PASS" to ""
        }
        if (status != "PASS") {
            throw peFailException(message)
        }
        return parsed
    }

    
    private fun peFailException(message: String): Exception {
        val msg = message.trim()
        val tokenLike = listOf("登录超时", "登录已过期", "会话过期", "重新登录", "未登录", "token失效", "令牌")
            .any { msg.contains(it, ignoreCase = true) }

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

    override suspend fun getFreeApplyDetail(applyId: String): PEFreeApplyDetailResponse {
        val sign = passwordEncryption.generatePESign(mapOf("applyId" to applyId))
        val requestBody = buildJsonObject {
            put("applyId", applyId)
            put("sign", sign)
        }
        val body = executeWithAuth(
            url = "$baseUrl/mobile/gymFreeManager/selectDetailData",
            requestBody = requestBody,
        )
        return parseAndVerify(body, PEFreeApplyDetailResponse.serializer())
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

    
    override suspend fun uploadPeFile(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): PEFreeActionResponse = withContext(AppIoDispatcher) {
        if (bytes.isEmpty()) throw IllegalStateException("文件内容为空")
        if (bytes.size > 5 * 1024 * 1024) throw IllegalStateException("每个文件上传大小不能超过5M")
        val token = authStore.getToken() ?: throw PETokenExpiredException("请先登录体测系统")

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

    
    override suspend fun downloadPeFile(attId: String): ByteArray = withContext(AppIoDispatcher) {
        if (attId.isBlank()) throw IllegalStateException("缺少附件标识")
        val token = authStore.getToken() ?: throw PETokenExpiredException("请先登录体测系统")
        val encAttId = passwordEncryption.encryptPeAttId(attId)
        if (encAttId.isBlank()) throw IllegalStateException("附件标识加密失败")

        val response: HttpResponse = httpClient.get(
            "$baseUrl/common/mobile/downloadFile?att_id=${encAttId.encodeURLParameter()}",
        ) {
            header("Authorization", token)
            header("Referer", "$baseUrl/mobile/")
        }

        when (response.headers["abnormal"]?.uppercase()) {
            "NOFUN" -> throw IllegalStateException("您没有操作权限")
            "TIMEOUT" -> throw PETokenExpiredException("登录超时，请重新登录")
        }
        if (response.status.value == 401) throw PETokenExpiredException("登录已过期，请重新登录")
        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                response.extractServerErrorMessage(json, listOf("message", "msg"))
                    ?: "下载失败（HTTP ${response.status.value}）",
            )
        }
        val bytes = response.body<ByteArray>()

        if (bytes.size < 8) throw IllegalStateException("附件内容为空")
        val head = bytes.decodeToString(0, minOf(16, bytes.size))
        if (head.startsWith("{") && head.contains("FAIL")) {
            throw IllegalStateException("附件下载失败")
        }
        bytes
    }

    override fun peFileDownloadUrl(attId: String): String {
        val encAttId = passwordEncryption.encryptPeAttId(attId)
        return "$baseUrl/common/mobile/downloadFile?att_id=${encAttId.encodeURLParameter()}"
    }

    private fun parseFreeActionOrThrow(body: String): PEFreeActionResponse {
        val parsed = json.decodeFromString(PEFreeActionResponse.serializer(), body)
        if (parsed.status != "PASS") {
            throw peFailException(parsed.message)
        }
        return parsed
    }
}
