package restarhalf.stellar.schedule.data.remote

import io.ktor.client.HttpClient
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
        val token = authStore.getToken() ?: throw PETokenExpiredException()

        val response: HttpResponse = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(requestBody)
        }

        if (response.status.value == 401) throw PETokenExpiredException()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                response.extractServerErrorMessage(json, listOf("message", "msg"))
                    ?: "请求失败（HTTP ${response.status.value}）"
            )
        }

        val body = response.bodyAsText()
        if (body.isBlank()) throw PETokenExpiredException()
        body
    }

    /**
     * 解析响应体并校验 status == "PASS"，否则抛出令牌过期异常。
     */
    private inline fun <reified T> parseAndVerify(
        body: String,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): T {
        val parsed = json.decodeFromString(deserializer, body)
        // 校验 status 字段（所有 PE 响应都有此字段）
        val status = when (parsed) {
            is PELoginResponse -> parsed.status
            is PEScoreListResponse -> parsed.status
            is PEDetailResponse -> parsed.status
            is PEAuthProfileResponse -> parsed.status
            else -> "PASS"
        }
        if (status != "PASS") {
            throw PETokenExpiredException()
        }
        return parsed
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
}
