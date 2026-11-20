package restarhalf.stellar.schedule.reminder

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.reminder.CourseReminderPlanner
import restarhalf.stellar.schedule.domain.reminder.CourseSlot
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import kotlinx.coroutines.CancellationException

/**
 * 上课自动勿扰入口：按当前课表算出「是否在课堂」后交给 [SessionDndController]，
 * 状态机（对账/快照/还原）与考试侧完全共用。
 */
internal object ClassDndController : KoinComponent {

    private val courseRepository: CourseRepository by inject()
    private val timetable: TimetablePort by inject()

    /** 闹钟 / 重排 / 开关切换 / 开机后共同调用，幂等。 */
    suspend fun applyCurrentState(context: Context, nowMs: Long = System.currentTimeMillis()) {
        val slots =
            try {
                todaySlots(nowMs)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                return
            }
        SessionDndController.applyCurrentState(
            context = context,
            session = ReminderSession.Course,
            inSession = isInClassNow(slots, nowMs),
            nowMs = nowMs,
        )
    }

    /** 开关关闭 / 课程提醒关闭：用户明确要求停止管理，无条件还原。 */
    fun handBackFromUser(context: Context) {
        SessionDndController.handBackFromUser(context, ReminderSession.Course)
    }

    /** 通知 / 超级岛「上课勿扰」按钮。 */
    suspend fun toggleFromNotification(context: Context) {
        val now = System.currentTimeMillis()
        val inClass =
            runCatching { isInClassNow(todaySlots(now), now) }.getOrDefault(false)
        SessionDndController.toggleFromNotification(context, ReminderSession.Course, inClass)
    }

    /** 重启 / 应用升级后清掉遗留的接管标记（课程与考试都清）。 */
    fun dropStaleSessionAfterReboot(context: Context) {
        SessionDndController.dropStaleSessionAfterReboot(context)
    }

    /**
     * 测试课（「测试小米超级岛」/「测试实时活动」）不在真实课表里，按毫秒级窗口并进来，
     * 否则勿扰链路在测试里跑不到。
     */
    private fun isInClassNow(slots: List<CourseSlot>, nowMs: Long): Boolean {
        val inTestClass =
            LiveReminderState.testClassWindow()
                ?.let { nowMs >= it.first && nowMs < it.second } == true
        return inTestClass || slots.any { nowMs >= it.startAtMs && nowMs < it.endAtMs }
    }

    private suspend fun todaySlots(nowMs: Long): List<CourseSlot> =
        CourseReminderPlanner.planDay(
            courses = courseRepository.getAllCoursesOnce(),
            campus = timetable.getCampus(),
            termStartMs = timetable.getTermStartMs(),
            totalWeeks = timetable.getTotalWeeks(),
            nowMs = nowMs,
            dayOffset = 0,
        )
}