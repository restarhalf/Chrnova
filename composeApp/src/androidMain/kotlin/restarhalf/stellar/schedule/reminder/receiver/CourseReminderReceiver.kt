package restarhalf.stellar.schedule.reminder.receiver

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.MainActivity
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextCourseReminderIfEnabledUseCase
import restarhalf.stellar.schedule.reminder.NotificationChannels
import java.util.Calendar

class CourseReminderReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()

        val name = intent.getStringExtra("course_name")
        if (name.isNullOrBlank()) {
            result.finish()
            return
        }
        val location = intent.getStringExtra("course_location") ?: ""
        val time = intent.getStringExtra("course_time") ?: ""
        val endTime = intent.getStringExtra("course_end_time") ?: ""

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // 频道统一由 NotificationChannels 建（原来这里和那里各建一份，改一处会漏另一处）
        NotificationChannels.ensureAll(context)

        // 小爱用 getDateNumber(msgSendTimestamp) 做通知 id：一天一条，同一天的多条提醒互相覆盖。
        // 这里用"日期 + 开始时间"的稳定哈希，同名课程不再互相顶掉。
        val notificationId = reminderNotificationId(nowMs = System.currentTimeMillis(), time = time)

        val launchIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val contentIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        // 用 Activity 而不是广播：MIUI 的 AudioHardening 只允许前台进程改铃声，
        // 后台广播里调 setRingerMode 会被静默忽略。见 MuteActionActivity。
        val muteIntent =
            MuteActionActivity.muteIntent(
                context = context,
                notificationId = notificationId,
                startTime = time,
                endTime = endTime,
            )
        val mutePendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val descText = buildString {
            if (time.isNotBlank()) append(time)
            if (location.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(location)
            }
        }

        val largeView =
            RemoteViews(context.packageName, R.layout.notify_reminder_large).apply {
                // 小爱这里两处是不同字段：notification_title 是卡片标题，detail_title 才是课程名
                setTextViewText(R.id.notify_title, CARD_TITLE)
                setTextViewText(R.id.notify_start_time, time)
                setTextViewText(R.id.notify_end_time, endTime)
                setTextViewText(R.id.notify_detail_title, name)
                setTextViewText(R.id.notify_detail_desc, location)
                setTextViewText(R.id.notify_button_text, MUTE_BUTTON_LABEL)
                setOnClickPendingIntent(R.id.notify_button_layout, mutePendingIntent)
                setOnClickPendingIntent(R.id.notify_root, contentIntent)
            }

        // 岛上的动作按钮：点击直接走静音流程，与自定义布局里的胶囊按钮同一个 PendingIntent。
        // 不带图标 —— 岛上的胶囊是纯文字按钮，塞图标反而会挤掉文字。
        // 文字用岛上的长版：走 Bundle 通道时系统 UI 取的是 Action.title，JSON 里的 actionTitle 不生效。
        val muteAction =
            Notification.Action.Builder(
                null,
                ISLAND_MUTE_LABEL,
                mutePendingIntent,
            ).build()

        // 小爱：不用 DecoratedCustomViewStyle，否则系统会在自定义布局上再叠一层自带标题栏
        val notification =
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(name)
                .setContentText(descText)
                // 收起态和横批都用大卡：时间列 + 竖线 + 课名/地点，信息一眼看全
                .setCustomContentView(largeView)
                .setCustomBigContentView(largeView)
                .setCustomHeadsUpContentView(largeView)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                // 小爱：上课提醒在下课后 10 分钟自动消失（getTimeoutAfter = 距开课 + 600000）
                .setTimeoutAfter(REMINDER_TIMEOUT_MS)
                .addAction(muteAction)
                .build()
        // 小爱给 MIUI 的通知卡片加了这两个 extras，自定义高度 + 允许展开
        notification.extras.putBoolean("miui.customHeight", true)
        notification.extras.putBoolean("miui.showAction", true)

        val nowMs = System.currentTimeMillis()
        val stage = clockStage(time = time, endTime = endTime, nowMs = nowMs)
        val timeRange = if (endTime.isBlank()) time else "$time | $endTime"
        val islandTitle = if (stage.label.isBlank()) rangeOrTime(time, endTime) else "${time}上课"
        val ticker =
            buildString {
                if (location.isNotBlank()) append(location).append("｜")
                // 中文是合法标识符字符，$time 后面紧跟"上课"必须加花括号，否则会被当成变量名
                append(if (stage.label.isBlank()) rangeOrTime(time, endTime) else "${time}上课")
            }
        val shareContent = buildString {
            appendLine(name)
            if (location.isNotBlank()) appendLine(location)
            append(rangeOrTime(time, endTime))
        }

        FocusNotification.attach(
            context = context,
            notification = notification,
            content =
                FocusContent(
                    courseName = name,
                    timeRange = timeRange,
                    statusLabel = stage.label,
                    timerType = stage.timerType,
                    timerWhenMs = stage.timerWhenMs,
                    placeLabel = PLACE_LABEL,
                    placeText = location,
                    islandTitle = islandTitle,
                    ticker = ticker,
                    shareContent = shareContent,
                    muteAction = muteAction,
                    muteTitle = ISLAND_MUTE_LABEL,
                ),
            nowMs = nowMs,
        )

        notificationManager.notify(notificationId, notification)

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val deps = Deps()
                deps.rescheduleNextReminderIfEnabled()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.log("Reminder", "重新调度下次提醒失败", e)
            } finally {
                result.finish()
            }
        }
    }

    private class Deps : KoinComponent {
        val rescheduleNextReminderIfEnabled: RescheduleNextCourseReminderIfEnabledUseCase by inject()
    }

    /**
     * 通知 id：同一天内按开课时间区分，跨天不冲突。
     *
     * 原来是 name.hashCode()，两门同名课程会互相覆盖 —— 小爱用日期做 id，
     * 这里在日期基础上再混入开课时间，既保留"一天一条"的语义又不丢课。
     */
    private fun reminderNotificationId(nowMs: Long, time: String): Int {
        val calendar =
            Calendar.getInstance().apply { timeInMillis = nowMs }
        val dayKey =
            calendar.get(Calendar.YEAR) * 1000 +
                calendar.get(Calendar.DAY_OF_YEAR)
        return (dayKey * 100 + (time.filter { it.isDigit() }.toIntOrNull() ?: 0)) and 0x7FFFFFFF
    }

    /**
     * 岛展开态左列的两行：标题 + 计时。
     *
     * 照着课程表超级岛的三段式：
     *   未开课 → "即将上课" + 到开课的倒计时
     *   上课中 → "距离下课" + 到下课的倒计时
     *   已下课 → "已经下课"，不带计时
     * 计时交给系统 UI 的 NotificationChronometerManager 走，它按 timerType / timerWhen
     * 自己每秒跳动，我们不用重发通知。
     */
    private fun clockStage(time: String, endTime: String, nowMs: Long): ClockStage {
        val startAt = parseTodayTimeMs(time)
        val endAt = if (endTime.isBlank()) null else parseTodayTimeMs(endTime)

        return when {
            startAt == null -> ClockStage(label = "")
            nowMs < startAt ->
                ClockStage(label = "即将上课", timerWhenMs = startAt)
            endAt != null && nowMs < endAt ->
                ClockStage(label = "距离下课", timerWhenMs = endAt)
            endAt == null ->
                ClockStage(label = "已经上课", timerWhenMs = startAt)
            else -> ClockStage(label = "已经下课")
        }
    }

    /** 没有跨天，只补上"开始-结束"这种连续区间；时间是偶发缺失的就直接回退成开始时间。 */
    private fun rangeOrTime(time: String, endTime: String): String =
        if (endTime.isBlank()) time else "$time-${endTime}"

    /**
     * @param timerWhenMs 计时基准时刻；0 表示不带计时
     */
    private data class ClockStage(
        val label: String,
        val timerWhenMs: Long = 0L,
    ) {
        val timerType: Int get() = if (timerWhenMs > 0L) TIMER_TYPE_COUNT_DOWN_RUN else 0
    }

    private fun parseTodayTimeMs(time: String): Long? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null

        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        return calendar.timeInMillis
    }

    companion object {

        const val CHANNEL_ID = "course_reminder_high"

        /** 小爱：距开课时间 + 10 分钟，即下课后自动收起 */
        private const val REMINDER_TIMEOUT_MS = 60 * 60 * 1000L
        private const val MUTE_BUTTON_LABEL = "静音"

        /** 岛上的胶囊按钮文字，比卡片上的"静音"更长 */
        private const val ISLAND_MUTE_LABEL = "上课静音"

        /** 系统 UI 的 NotificationChronometerManager：-1 = 倒计时跑动中 */
        private const val TIMER_TYPE_COUNT_DOWN_RUN = -1

        /** 展开态大卡的标题（小爱是服务端下发的卡片标题，这里用固定文案代替） */
        private const val CARD_TITLE = "上课提醒"

        /** 岛右列的标题，与课程表超级岛一致 */
        private const val PLACE_LABEL = "地点"
    }
}
