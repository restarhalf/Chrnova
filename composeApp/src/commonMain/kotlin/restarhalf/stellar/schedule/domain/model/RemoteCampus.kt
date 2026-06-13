package restarhalf.stellar.schedule.domain.model

/**
 * 远程校区数据模型
 * 
 * 表示从教务系统获取的校区信息。
 */
data class RemoteCampus(
    /** 校区ID */
    val id: String,
    /** 校区名称 */
    val name: String,
    /** 是否为默认校区 */
    val isDefault: Boolean,
)
