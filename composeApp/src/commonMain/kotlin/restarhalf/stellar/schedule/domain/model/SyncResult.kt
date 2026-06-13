package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.Serializable

/**
 * 教务系统同步结果数据模型
 * 
 * 记录从教务系统同步课程数据后的结果信息。
 */
@Serializable
data class SyncResult(
    /** 新插入的课程数量 */
    val inserted: Int,
    /** 学期ID */
    val semesterId: String,
    /** 校区ID */
    val campusId: String,
    /** 校区名称 */
    val campusName: String,
    /** 当前周次（如"第5周"） */
    val week: String,
)
