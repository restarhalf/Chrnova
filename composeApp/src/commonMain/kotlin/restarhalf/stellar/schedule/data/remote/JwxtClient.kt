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

    private suspend fun executePostEmpty(url: String): String =
        withContext(AppIoDispatcher) {
            val response: HttpResponse = httpClient.post(url)
            if (!response.status.isSuccess()) {
                throw IllegalStateException("请求失败（HTTP ${response.status.value}）")
            }
            response.body()
        }

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

            val url = "http://jwyd.dlnu.edu.cn/njwhd/login"

            val response: HttpResponse =
                httpClient.submitForm(url = url, formParameters = parameters)

            if (!response.status.isSuccess()) {
                throw IllegalStateException("登录失败（HTTP ${response.status.value}）")
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

    override suspend fun getCurrentTerm(): JwxtCurrentTermResponse =
        withContext(AppIoDispatcher) {
            val body = executePostEmpty("http://jwyd.dlnu.edu.cn/njwhd/currentTerm")
            json.decodeFromString(
                JwxtApiResponse.serializer(ListSerializer(JwxtCurrentTermItem.serializer())), body
            )
        }

    override suspend fun getSemesterList(): List<JwxtSemesterItem> =
        withContext(AppIoDispatcher) {
            val body = executePostEmpty("http://jwyd.dlnu.edu.cn/njwhd/getXnxqList")
            json.decodeFromString(ListSerializer(JwxtSemesterItem.serializer()), body)
        }

    override suspend fun getTeachingWeek(): JwxtTeachingWeekResponse =
        withContext(AppIoDispatcher) {
            val body = executePostEmpty("http://jwyd.dlnu.edu.cn/njwhd/teachingWeek")
            json.decodeFromString(JwxtTeachingWeekResponse.serializer(), body)
        }

    override suspend fun getCampusList(): JwxtCampusResponse =
        withContext(AppIoDispatcher) {
            val body = executePostEmpty("http://jwyd.dlnu.edu.cn/njwhd/Get_sjkbms")
            json.decodeFromString(JwxtCampusResponse.serializer(), body)
        }

    override suspend fun fetchCurriculum(fields: Map<String, String>): JwxtCurriculumResponse =
        withContext(AppIoDispatcher) {
            val url = "http://jwyd.dlnu.edu.cn/njwhd/student/curriculum"

            val response: HttpResponse =
                httpClient.post(url) {
                    fields["xnxq01id"]?.takeIf { it.isNotBlank() }
                        ?.let { parameter("xnxq01id", it) }
                    fields["kbjcmsid"]?.takeIf { it.isNotBlank() }
                        ?.let { parameter("kbjcmsid", it) }
                    fields["week"]?.takeIf { it.isNotBlank() }?.let { parameter("week", it) }
                }

            if (!response.status.isSuccess()) {
                throw IllegalStateException("课表请求失败（HTTP ${response.status.value}）")
            }
            response.body()
        }

    override suspend fun fetchExaminationArrangement(
        semester: String,
        nameOrNumber: String
    ): JwxtExaminationResponse =
        withContext(AppIoDispatcher) {
            val url = "http://jwyd.dlnu.edu.cn/njwhd/student/examinationArrangement"

            val response: HttpResponse =
                httpClient.post(url) {
                    parameter("semester", semester)
                    parameter("nameOrNumber", nameOrNumber)
                }

            if (!response.status.isSuccess()) {
                throw IllegalStateException("考试安排请求失败（HTTP ${response.status.value}）")
            }
            response.body()
        }

    override suspend fun fetchTermGradeReport(semester: String): JwxtTermGradeResponse =
        withContext(AppIoDispatcher) {
            val url = "http://jwyd.dlnu.edu.cn/njwhd/student/termGPA"

            val response: HttpResponse =
                httpClient.post(url) {
                    parameter("semester", semester)
                    parameter("type", "1")
                }

            if (!response.status.isSuccess()) {
                throw IllegalStateException("成绩请求失败（HTTP ${response.status.value}）")
            }
            json.decodeFromString(
                JwxtApiResponse.serializer(ListSerializer(JwxtTermGradeDataItem.serializer())),
                response.body<String>()
            )
        }

    private fun extractServerIdFromSetCookie(setCookies: List<String>): String? {
        for (header in setCookies) {
            val parts = header.split(';')
            val keyValue = parts.firstOrNull().orEmpty().trim()
            if (keyValue.startsWith("SERVERID=", ignoreCase = true)) {
                return keyValue
            }
        }
        return null
    }
}
