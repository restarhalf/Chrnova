package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable

/**
 * 用户认证档案数据模型
 *
 * 存储从教务系统获取的用户基本信息，用于展示和身份识别。
 */
@Immutable
data class AuthProfile(
    /** 学生姓名 */
    val name: String = "",
    /** 学号 */
    val userNo: String = "",
    /** 班级名称 */
    val clsName: String = "",
    /** 学院名称 */
    val academyName: String = "",
)
