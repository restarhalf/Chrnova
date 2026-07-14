package restarhalf.stellar.schedule.reminder

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.usecase.RescheduleRemindersUseCase
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderRescheduleWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {

    private val rescheduleReminders: RescheduleRemindersUseCase by inject()

    override suspend fun doWork(): Result {
        return try {
            when (rescheduleReminders()) {
                RescheduleRemindersUseCase.Result.Success -> Result.success()
                RescheduleRemindersUseCase.Result.CourseReminderFailed -> Result.retry()
                RescheduleRemindersUseCase.Result.ExamReminderFailed -> Result.retry()
                RescheduleRemindersUseCase.Result.BothRemindersFailed -> Result.retry()
            }
        } catch (e: IOException) {
            AppLogger.log("Reminder", "重新调度提醒IO失败", e)
            Result.retry()
        } catch (e: IllegalStateException) {
            AppLogger.log("Reminder", "重新调度提醒数据状态异常", e)
            Result.failure()
        } catch (e: Exception) {
            AppLogger.log("Reminder", "重新调度提醒意外失败", e)
            Result.failure()
        }
    }
}

object ReminderWorkScheduler {
    private const val UNIQUE_WORK_NAME = "daily_reminder_reschedule"
    private const val UNIQUE_WORK_NAME_PERIODIC_CHECK = "periodic_reminder_check"
    private const val UNIQUE_WORK_NAME_ONCE = "reminder_reschedule_once"

    private fun createConstraints(): Constraints =
        Constraints.Builder()
            .setRequiresBatteryNotLow(true) // 低电量时不执行，省电
            .build()

    private fun computeInitialDelayMs(targetHour: Int = 5, targetMinute: Int = 10): Long {
        val now = System.currentTimeMillis()
        val cal =
            Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis - now
    }

    fun enqueueDaily(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<ReminderRescheduleWorker>(1, TimeUnit.DAYS)
                .setConstraints(createConstraints())
                .setInitialDelay(computeInitialDelayMs(), TimeUnit.MILLISECONDS)
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)


        enqueuePeriodicCheck(context)
    }

    fun enqueuePeriodicCheck(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<ReminderRescheduleWorker>(6, TimeUnit.HOURS)
                .setConstraints(createConstraints())
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME_PERIODIC_CHECK, ExistingPeriodicWorkPolicy.KEEP, request
            )
    }

    fun enqueueNow(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<ReminderRescheduleWorker>()
                .setConstraints(createConstraints())
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME_ONCE, ExistingWorkPolicy.REPLACE, request)
    }
}
