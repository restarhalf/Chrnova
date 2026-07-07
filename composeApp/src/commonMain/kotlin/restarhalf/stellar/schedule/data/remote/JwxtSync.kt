package restarhalf.stellar.schedule.data.remote

import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 教务系统同步服务
 * 
 * 负责从教务系统获取课程数据并解析为领域模型。
 * 
 * @param gateway 教务系统网关
 */
class JwxtSync(private val gateway: JwxtGateway) {

    /**
     * 获取课程列表
     * 
     * @param semesterId 学期ID
     * @param campusId 校区ID
     * @param week 周次筛选，默认"all"表示所有周次
     * @return 解析后的课程列表
     * @throws IllegalStateException 获取失败时抛出
     */
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

    /**
     * 获取学期开始日期（第一周周一的日期）
     *
     * 通过请求 week=1 获取第一周的日期数据，提取周一（xqid=1）的 mxrq 作为学期开始日期。
     *
     * @param semesterId 学期ID
     * @param campusId 校区ID
     * @return 学期开始时间戳（毫秒），获取失败时返回 null
     */
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
                            "week" to "1"
                        )
                    )
                if (!response.isSuccess()) return@withContext null

                val mondayDate = response.data
                    .flatMap { it.date }
                    .firstOrNull { it.xqid == 1 && it.mxrq.isNotBlank() }
                    ?.mxrq
                    ?: return@withContext null

                val date = LocalDate.parse(mondayDate)
                date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }.getOrNull()
        }
}
