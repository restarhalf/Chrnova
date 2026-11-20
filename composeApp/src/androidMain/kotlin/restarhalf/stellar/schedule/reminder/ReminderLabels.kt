package restarhalf.stellar.schedule.reminder

/**
 * 岛/实况/勿扰共用的文案与开关口径。课程与考试各一套，
 * 其余链路（三态、对账、补发、快照还原）完全一致。
 */
internal data class ReminderLabels(
    /** 通知上的动作按钮：查看课表 / 查看考试 */
    val viewAction: String,
    /** 通知与岛上的勿扰按钮：上课勿扰 / 考试勿扰 */
    val dndAction: String,
    /** 实况正文后缀：N分钟后上课 / N分钟后开考 */
    val preClassSuffix: String,
    /** 岛缩略态到点文案：已上课 / 已开考 */
    val started: String,
    /** 岛右侧倒计时前缀：距上课 / 距开考 */
    val startsIn: String,
    /** 岛展开态课前状态：即将上课 / 即将开考 */
    val upcoming: String,
    /** 实况「现在上课」/「现在开考」 */
    val nowSession: String,
    /** 进行中：正在上课 / 正在考试 */
    val inSessionNow: String,
    /** 岛展开态进行中倒计时：距离下课 / 距离结束 */
    val untilSessionEnds: String,
    /** 岛右侧进行中倒计时前缀：距下课 / 距结束 */
    val endsIn: String,
    /** 已结束：已下课 / 已结束 */
    val sessionEnded: String,
    /** 实况进行中信息行后缀：下课 / 结束 */
    val endsAtSuffix: String,
    /** 实况无结束时间时的进行中信息行：距下课 / 距结束 */
    val distanceToEnd: String,
    /** 地点标签：地点 / 考场 */
    val placeLabel: String,
    /** 超级岛 business 字段 */
    val business: String,
)

internal val CourseLabels = ReminderLabels(
    viewAction = "查看课表",
    dndAction = "上课勿扰",
    preClassSuffix = "分钟后上课",
    started = "已上课",
    startsIn = "距上课",
    upcoming = "即将上课",
    nowSession = "现在上课",
    inSessionNow = "正在上课",
    untilSessionEnds = "距离下课",
    endsIn = "距下课",
    sessionEnded = "已下课",
    endsAtSuffix = "下课",
    distanceToEnd = "距下课",
    placeLabel = "地点",
    business = "course_reminder",
)

internal val ExamLabels = ReminderLabels(
    viewAction = "查看考试",
    dndAction = "考试勿扰",
    preClassSuffix = "分钟后开考",
    started = "已开考",
    startsIn = "距开考",
    upcoming = "即将开考",
    nowSession = "现在开考",
    inSessionNow = "正在考试",
    untilSessionEnds = "距离结束",
    endsIn = "距结束",
    sessionEnded = "已结束",
    endsAtSuffix = "结束",
    distanceToEnd = "距结束",
    placeLabel = "考场",
    business = "exam_reminder",
)