package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import restarhalf.stellar.schedule.domain.port.SettingsPort

/**
 * 同步考试事件到日历用例
 *
 * 全量写入:先删除本应用之前写入的所有考试事件,再按当前本地数据库中的考试列表重新写入。
 * 若未开启"考试日历提醒",直接返回 Success(0)。
 */
class SyncExamEventsToCalendarUseCase(
    private val observeAllExaminations: ObserveAllExaminationsUseCase,
    private val calendarEvent: CalendarEventPort,
    private val settings: SettingsPort,
) {
    /**
     * @param selectedTerm 选中的学期,仅用于过滤(空字符串表示不按学期过滤)
     */
    suspend operator fun invoke(selectedTerm: String): CalendarEventPort.SyncResult {
        val enabled = settings.observeExamReminderEnabled().first()
        if (!enabled) return CalendarEventPort.SyncResult.Success(0)
        if (!calendarEvent.hasCalendarPermission()) {
            return CalendarEventPort.SyncResult.PermissionDenied
        }
        val exams = observeAllExaminations().first()
        val filtered = if (selectedTerm.isBlank()) exams
            else exams.filter { it.semesterId == selectedTerm }
        return calendarEvent.syncExamEvents(exams = filtered)
    }
}
