package restarhalf.stellar.schedule.data.remote

import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
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

            buildList {
                for (group in response.data) {
                    for (item in group.item) {
                        addAll(JwxtTimeParser.parseToCourses(item))
                    }
                }
            }
        }

    suspend fun fetchTermStartDate(
        semesterId: String,
        campusId: String,
    ): Long? =
        withContext(AppIoDispatcher) {
            runCatching {
                val response =
                    gateway.fetchCurriculum(
                        fields = mapOf(
                            "xnxq01id" to semesterId,
                            "kbjcmsid" to campusId,
                            "week" to "2"
                        )
                    )
                if (!response.isSuccess()) return@withContext null

                val mondayDate = response.data
                    .flatMap { it.date }
                    .firstOrNull { it.xqid == 1 && it.mxrq.isNotBlank() }
                    ?.mxrq
                    ?: return@withContext null

                val date = LocalDate.parse(mondayDate).minus(7, DateTimeUnit.DAY)
                date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }.getOrNull()
        }
}
