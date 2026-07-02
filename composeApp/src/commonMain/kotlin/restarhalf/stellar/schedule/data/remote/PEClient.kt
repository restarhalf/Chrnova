package restarhalf.stellar.schedule.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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

    override suspend fun login(username: String, password: String): PELoginResponse =
        withContext(AppIoDispatcher) {
            val encryptedPassword = passwordEncryption.encryptPasswordForPELogin(password)
            val sign = generateSign(
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
                throw IllegalStateException("登录失败（HTTP ${response.status.value}）")
            }

            val parsed = json.decodeFromString(PELoginResponse.serializer(), response.body())
            if (parsed.status == "PASS") {
                parsed.token?.let { authStore.setToken(it) }
                parsed.userId?.let { authStore.setUserId(it) }
                authStore.setUsername(username)
                authStore.setPassword(password)
            }
            parsed
        }

    override suspend fun getScoreList(): PEScoreListResponse =
        withContext(AppIoDispatcher) {
            val userId = authStore.getUserId() ?: throw PETokenExpiredException()
            val token = authStore.getToken() ?: throw PETokenExpiredException()
            val sign = generateSign(mapOf("user_id" to userId))

            val requestBody = buildJsonObject {
                put("user_id", userId)
                put("sign", sign)
            }

            val response: HttpResponse =
                httpClient.post("$baseUrl/mobile/gymResult/selectUserPlanList") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", token)
                    setBody(requestBody)
                }

            if (response.status.value == 401) {
                throw PETokenExpiredException()
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("获取成绩列表失败（HTTP ${response.status.value}）")
            }

            val body = response.bodyAsText()
            if (body.isBlank()) {
                throw PETokenExpiredException("登录已过期，请重新登录")
            }

            val parsed = json.decodeFromString(PEScoreListResponse.serializer(), body)
            if (parsed.status != "PASS") {
                throw PETokenExpiredException("登录已过期，请重新登录")
            }
            parsed
        }

    override suspend fun getScoreDetail(schoolYear: String): PEDetailResponse =
        withContext(AppIoDispatcher) {
            val userId = authStore.getUserId() ?: throw PETokenExpiredException()
            val token = authStore.getToken() ?: throw PETokenExpiredException()
            val sign = generateSign(mapOf("user_id" to userId, "school_year" to schoolYear))

            val requestBody = buildJsonObject {
                put("user_id", userId)
                put("school_year", schoolYear)
                put("sign", sign)
            }

            val response: HttpResponse =
                httpClient.post("$baseUrl/mobile/gymResult/selectUserPlanScore") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", token)
                    setBody(requestBody)
                }

            if (response.status.value == 401) {
                throw PETokenExpiredException()
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("获取成绩详情失败（HTTP ${response.status.value}）")
            }

            val body = response.bodyAsText()
            if (body.isBlank()) {
                throw PETokenExpiredException("登录已过期，请重新登录")
            }

            val parsed = json.decodeFromString(PEDetailResponse.serializer(), body)
            if (parsed.status != "PASS") {
                throw PETokenExpiredException("登录已过期，请重新登录")
            }
            parsed
        }

    override suspend fun getStudentInfo(): PEStudentInfoResponse =
        withContext(AppIoDispatcher) {
            val userId = authStore.getUserId() ?: throw PETokenExpiredException()
            val token = authStore.getToken() ?: throw PETokenExpiredException()
            val sign = generateSign(mapOf("userId" to userId))

            val requestBody = buildJsonObject {
                put("userId", userId)
                put("sign", sign)
            }

            val response: HttpResponse = httpClient.post("$baseUrl/sysUser/mobile/findStudent") {
                contentType(ContentType.Application.Json)
                header("Authorization", token)
                setBody(requestBody)
            }

            if (response.status.value == 401) {
                throw PETokenExpiredException()
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("获取学生信息失败（HTTP ${response.status.value}）")
            }

            val body = response.bodyAsText()
            if (body.isBlank()) {
                throw PETokenExpiredException("登录已过期，请重新登录")
            }

            val parsed = json.decodeFromString(PEStudentInfoResponse.serializer(), body)
            if (parsed.status != "PASS") {
                throw PETokenExpiredException("登录已过期，请重新登录")
            }
            parsed
        }

    private fun generateSign(data: Map<String, Any?>): String {
        return passwordEncryption.generatePESign(data)
    }
}
