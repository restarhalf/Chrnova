package restarhalf.stellar.schedule.reminder

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.domain.reminder.ExamReminderPlanner
import restarhalf.stellar.schedule.domain.reminder.ExamSlot
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlinx.coroutines.CancellationException

/**
 * 考试自动勿扰入口：按当前考试安排算出「是否在考场」后交给 [SessionDndController]，
 * 状态机（对账/快照/还原）与课程侧完全共用，照 [ClassDndController]。
 */
internal object ExamDndController : KoinComponent {

    private val examinationRepository: ExaminationRepository by inject()

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
            session = ReminderSession.Exam,
            inSession = slots.any { nowMs >= it.startAtMs && nowMs < it.endAtMs },
            nowMs = nowMs,
        )
    }

    /** 开关关闭 / 提醒总开关关闭：用户明确要求停止管理，无条件还原。 */
    fun handBackFromUser(context: Context) {
        SessionDndController.handBackFromUser(context, ReminderSession.Exam)
    }

    /** 通知 / 超级岛「考试勿扰」按钮。 */
    suspend fun toggleFromNotification(context: Context) {
        val now = System.currentTimeMillis()
        val inExam =
            runCatching {
                todaySlots(now).any { now >= it.startAtMs && now < it.endAtMs }
            }.getOrDefault(false)
        SessionDndController.toggleFromNotification(context, ReminderSession.Exam, inExam)
    }

    private suspend fun todaySlots(nowMs: Long): List<ExamSlot> =
        ExamReminderPlanner.planDay(
            exams = examinationRepository.getAllExaminationsOnce(),
            nowMs = nowMs,
            dayOffset = 0,
        )
}