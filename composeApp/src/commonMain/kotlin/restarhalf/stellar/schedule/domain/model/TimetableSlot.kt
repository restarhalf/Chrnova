package restarhalf.stellar.schedule.domain.model

/**
 * 课程节次时间槽数据模型
 * 
 * 定义每节课的开始和结束时间，用于课程表时间轴展示。
 */
data class TimetableSlot(
    /** 节次编号（从1开始） */
    val num: Int,
    /** 开始时间（如"08:00"） */
    val start: String,
    /** 结束时间（如"08:45"） */
    val end: String,
)
