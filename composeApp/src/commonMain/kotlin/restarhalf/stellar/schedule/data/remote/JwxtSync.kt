package restarhalf.stellar.schedule.data.remote

import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.platform.AppIoDispatcher

class JwxtSync(private val gateway: JwxtGateway) {

    suspend fun fetchCourses(
        semesterId: String,
        campusId: String,
        week: String = "all",
    ): List<JwxtTimeParser.ParsedCourse> =
        withContext(AppIoDispatcher) {
            val response =
                gateway.fetchCurriculum(
                    fields = mapOf("xnxq01id" to semesterId, "kbjcmsid" to campusId, "week" to week)
                )

            if (!response.isSuccess()) {
                throw IllegalStateException(response.msg.ifBlank { "课表接口返回失败" })
            }

            response.data.flatMap { it.item }.flatMap { JwxtTimeParser.parseToCourses(it) }
        }
}
