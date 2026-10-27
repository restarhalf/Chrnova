package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * 一键重排：先清掉已挂的闹钟，再按当前课表/考试重新排。
 *
 * 和对外的「每日兜底」不同，这里不做 hasScheduledAlarm 判断——用户点这个按钮的意思
 * 就是「不管现在挂了什么，按最新数据重算一遍」。
 */
class RescheduleAllRemindersUseCase(
    private val settings: ReminderSettingsPort,
    private val courseRepository: CourseRepository,
    private val timetable: TimetablePort,
    private val courseReminder: CourseReminderPort,
    private val examReminder: ExamReminderPort,
    private val collectReminderExaminations: CollectReminderExaminationsUseCase,
    private val cancelAllCourseReminders: CancelAllCourseRemindersUseCase,
    private val cancelAllExamReminders: CancelAllExamRemindersUseCase,
) {

    data class Report(
        val courseEnabled: Boolean,
        val courseResult: CourseReminderPort.ScheduleResult?,
        val examEnabled: Boolean,
        val examResult: ExamReminderPort.ScheduleResult?,
        /** 考试拉取失败（多为网络/登录问题），此时课程提醒仍然照常重排 */
        val examFetchFailed: Boolean,
    )

    suspend operator fun invoke(): Report {
        val courseEnabled = settings.observeCourseReminderEnabled().first()
        val examEnabled = settings.observeExamReminderEnabled().first()

        cancelAllCourseReminders()
        cancelAllExamReminders()

        val courseResult = if (courseEnabled) scheduleCourse() else null

        var examResult: ExamReminderPort.ScheduleResult? = null
        var examFetchFailed = false
        if (examEnabled) {
            val collected = collectReminderExaminations(semester = "", nameOrNumber = "")
            // 教务没拉到、本地库也是空的，才算真的没法排
            if (collected.remoteFailed && collected.exams.isEmpty()) {
                examFetchFailed = true
            } else {
                examResult =
                    runCatching { examReminder.scheduleNextReminder(collected.exams) }
                        .onFailure {
                            if (it is kotlinx.coroutines.CancellationException) throw it
                            AppLogger.log("Reminder", "一键重排：考试提醒调度失败", it)
                        }
                        .getOrNull()
            }
        }

        return Report(
            courseEnabled = courseEnabled,
            courseResult = courseResult,
            examEnabled = examEnabled,
            examResult = examResult,
            examFetchFailed = examFetchFailed,
        )
    }

    private suspend fun scheduleCourse(): CourseReminderPort.ScheduleResult? =
        runCatching {
            courseReminder.scheduleNextReminder(
                courses = courseRepository.getAllCoursesOnce(),
                campus = timetable.getCampus(),
                termStartMs = timetable.getTermStartMs(),
                totalWeeks = timetable.getTotalWeeks(),
            )
        }
            .onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                AppLogger.log("Reminder", "一键重排：课程提醒调度失败", it)
            }
            .getOrNull()
}
