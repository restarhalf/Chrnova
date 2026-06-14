package restarhalf.stellar.schedule.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 屏幕路由密封接口
 * 
 * 定义应用中所有页面的路由，实现Navigation3的NavKey接口。
 * 使用@Serializable注解支持路由参数序列化。
 */
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
     */
    @Serializable
    data class ClassEdit(val courseId: Long? = null) : Screen

}
