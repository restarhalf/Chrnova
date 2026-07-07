package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.SyncResult

/**
 * 同步端口接口
 * 
 * 定义教务系统数据同步的抽象接口。
 */
interface SyncPort {
    /**
     * 从教务系统同步课程数据
     *
     * @param semesterId 学期ID
     * @param campusId 校区ID
     * @param week 周次筛选，默认"all"表示同步所有周次
     * @return 同步结果，包含插入的课程数量等信息
     */
    suspend fun sync(semesterId: String, campusId: String, week: String = "all"): SyncResult

    /**
     * 获取学期开始日期（第一周周一的日期）
     *
     * @param semesterId 学期ID
     * @param campusId 校区ID
     * @return 学期开始时间戳（毫秒），获取失败时返回 null
     */
    suspend fun fetchTermStartDate(semesterId: String, campusId: String): Long?
}
