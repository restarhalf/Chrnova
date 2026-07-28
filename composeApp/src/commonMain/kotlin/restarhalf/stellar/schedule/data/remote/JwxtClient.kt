package restarhalf.stellar.schedule.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
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
}
