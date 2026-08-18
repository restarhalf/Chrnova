package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable

/**
 * 体育系统用户档案数据模型
 *
 * 存储从体育系统获取的用户基本信息，用于展示和身份识别。
 */
@Immutable
data class PEAuthProfile(
    /** 学生姓名 */
    val stuName: String = "",
    /** 学号 */
    val stdNumber: String = "",
    /** 测试码 */
    val testCode: String = "",
)
