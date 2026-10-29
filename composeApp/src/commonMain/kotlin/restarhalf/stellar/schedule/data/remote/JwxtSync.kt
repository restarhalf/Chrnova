package restarhalf.stellar.schedule.data.remote

import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import restarhalf.stellar.schedule.core.error.ParseFailureException
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

            val groups = response.data
                ?: throw ParseFailureException("课表数据解析失败：缺少 data 字段")

            var rawItemCount = 0
            val courses = buildList {
                for (group in groups) {
                    for (item in group.item) {
                        rawItemCount++
                        addAll(JwxtTimeParser.parseToCourses(item))
                    }
                }
            }

            if (rawItemCount > 0 && courses.isEmpty()) {
                throw ParseFailureException("课表数据解析失败：$rawItemCount 条课程均无法解析")
            }
            courses
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

                val mondayDate = response.data.orEmpty()
                    .flatMap { it.date }
                    .firstOrNull { it.xqid == 1 && it.mxrq.isNotBlank() }
                    ?.mxrq
                    ?: return@withContext null

                val date = LocalDate.parse(mondayDate).minus(7, DateTimeUnit.DAY)
                date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }.getOrNull()
        }
}
