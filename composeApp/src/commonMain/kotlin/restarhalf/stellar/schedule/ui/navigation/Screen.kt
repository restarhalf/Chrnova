package restarhalf.stellar.schedule.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 屏幕路由密封接口
 * 
 * 定义应用中所有页面的路由，实现miuix-nav的NavKey接口。
 * 使用@Serializable注解支持返回栈的保存/恢复（进程死亡后恢复）。
 */
@Serializable
sealed interface Screen : NavKey {
    /** 主页面容器（包含底部导航栏） */
    @Serializable
    data object Main : Screen

    /** 首页 - 显示今日课程 */
    @Serializable
    data object Home : Screen

    /** 课程表页面 - 按周查看完整课表 */
    @Serializable
    data object Schedule : Screen

    /** 考试与成绩页面 - 显示考试安排和成绩 */
    @Serializable
    data object EMS : Screen

    /** 设置页面 - 应用设置和账号管理 */
    @Serializable
    data object Settings : Screen

    /** 更换背景页面 - 背景图片和透明度设置 */
    @Serializable
    data object ChangeBackground : Screen

    /** 关于页面 - 版本信息和更新检查 */
    @Serializable
    data object About : Screen

    /** 体育成绩页面 - 体测成绩列表 */
    @Serializable
    data object PEScore : Screen

    /**
     * 体育成绩详情页面
     * 
     * @param schoolYear 学年（如"2023-2024"）
     */
    @Serializable
    data class PEDetail(val schoolYear: String) : Screen

    /** 日志页面 */
    @Serializable
    data object Log : Screen

    /**
     * 课程编辑页面
     * 
     * @param courseId 课程ID，null表示新建实验课
     * @param dayOfWeek 预选星期几（1-7），用于新建时预填
     * @param startSection 预选开始节次（1-12），用于新建时预填
     * @param selectedWeek 预选周次（1-n），用于新建时预填
     */
    @Serializable
    data class ClassEdit(val courseId: Long? = null, val dayOfWeek: Int = 1, val startSection: Int = 1, val selectedWeek: Int = 1) : Screen

    /**
     * 考试编辑页面
     * 
     * @param examinationId 考试ID，null表示新建考试
     */
    @Serializable
    data class ExamEdit(val examinationId: Long? = null) : Screen

    @Serializable
    data object Papers : Screen

    @Serializable
    data class PapersDetail(val paperId: String) : Screen

    @Serializable
    data object PapersUpload : Screen

    /** 课程评价列表页（课程聚合视图） */
    @Serializable
    data object Evaluation : Screen

    /** 某课程某教师的评价列表页（两层结构的第二层） */
    @Serializable
    data class EvaluationCourse(
        val courseName: String,
        val teacher: String = "",
    ) : Screen

    /** 课程评价详情页 */
    @Serializable
    data class EvaluationDetail(val evaluationId: String) : Screen

    /**
     * 课程评价提交/编辑页
     *
     * @param evaluationId 待编辑评价 ID；null 表示新建评价
     */
    @Serializable
    data class EvaluationSubmit(val evaluationId: String? = null) : Screen

    /** 教务系统登录页面 */
    @Serializable
    data object JwxtLogin : Screen

    /** 体育系统登录页面 */
    @Serializable
    data object PELogin : Screen

    /** 体育二维码页面 */
    @Serializable
    data object PEQRCode : Screen

    /** 个人资料页面 */
    @Serializable
    data object Profile : Screen

    /** 选修课学分统计页面 */
    @Serializable
    data object ElectiveCredit : Screen

    /** 美食滚轮页面 */
    @Serializable
    data object FoodRoulette : Screen

    /**
     * 美食二维码页面
     *
     * @param foodName 食物名称
     * @param qrContent 二维码内容
     */
    @Serializable
    data class FoodQRCode(
        val foodName: String,
        val qrContent: String = "",
    ) : Screen

    /** 自动抢课页面 */
    @Serializable
    data object CourseSelection : Screen

    /** 公告列表页 */
    @Serializable
    data object AnnouncementList : Screen

    /**
     * 公告详情页
     *
     * @param announcementId 公告唯一标识
     */
    @Serializable
    data class AnnouncementDetail(val announcementId: String) : Screen

}
