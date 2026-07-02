package restarhalf.stellar.schedule.data.remote

import kotlinx.coroutines.withContext
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
}
