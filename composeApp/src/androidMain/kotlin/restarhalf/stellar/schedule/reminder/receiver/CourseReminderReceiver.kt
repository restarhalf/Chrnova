package restarhalf.stellar.schedule.reminder.receiver

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.widget.RemoteViews
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.MainActivity
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextCourseReminderIfEnabledUseCase
import java.util.Calendar

class CourseReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()

        if (intent.action == ACTION_MUTE_DEVICE) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val previousMode = audioManager.ringerMode
                val startTime = intent.getStringExtra(EXTRA_COURSE_START_TIME).orEmpty()
                val endTime = intent.getStringExtra(EXTRA_COURSE_END_TIME).orEmpty()
                val notificationId = intent.getIntExtra("notification_id", -1)
                val now = System.currentTimeMillis()
                val startAtMs = parseTodayTimeMs(startTime)
                val endAtMs = parseTodayTimeMs(endTime)

                when {
                    startAtMs != null && now < startAtMs -> {
                        scheduleClassMute(
                            context = context,
                            notificationId = notificationId,
                            startAtMs = startAtMs,
                            endTime = endTime,
                            restoreMode = previousMode
                        )
                        Toast.makeText(context, "已设置上课静音，到点自动开启", Toast.LENGTH_SHORT)
                            .show()
                    }

                    endAtMs != null && now in (startAtMs ?: now)..endAtMs -> {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        scheduleAutoUnmute(
                            context = context,
                            notificationId = notificationId,
                            endTime = endTime,
                            restoreMode = previousMode
                        )
                        Toast.makeText(context, "当前在上课时段，已开启静音", Toast.LENGTH_SHORT)
                            .show()
                    }

                    else -> {
                        Toast.makeText(context, "当前不在上课时段，未开启静音", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                if (notificationId != -1) {
                    val notificationManager =
                        context.getSystemService(NotificationManager::class.java)
                    notificationManager.cancel(notificationId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "静音失败，可能需要免打扰权限", Toast.LENGTH_SHORT).show()
            }
            result.finish()
            return
        }

        if (intent.action == ACTION_UNMUTE_DEVICE) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val restoreMode =
                    intent.getIntExtra(EXTRA_RESTORE_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
                audioManager.ringerMode = restoreMode
                Toast.makeText(context, "课程已结束，已恢复铃声", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            result.finish()
            return
        }

        if (intent.action == ACTION_APPLY_CLASS_MUTE) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val endTime = intent.getStringExtra(EXTRA_COURSE_END_TIME).orEmpty()
                val restoreMode =
                    intent.getIntExtra(EXTRA_RESTORE_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
                val notificationId = intent.getIntExtra("notification_id", -1)
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                scheduleAutoUnmute(
                    context = context,
                    notificationId = notificationId,
                    endTime = endTime,
                    restoreMode = restoreMode
                )
                Toast.makeText(context, "已到上课时间，自动开启静音", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            result.finish()
            return
        }

        val name = intent.getStringExtra("course_name")
        if (name.isNullOrBlank()) {
            result.finish()
            return
        }
        val location = intent.getStringExtra("course_location") ?: ""
        val time = intent.getStringExtra("course_time") ?: ""
        val endTime = intent.getStringExtra(EXTRA_COURSE_END_TIME) ?: ""

        val notificationManager = context.getSystemService(NotificationManager::class.java)


        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "课程提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "上课前推送通知提醒"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 150, 200)
                    setShowBadge(true)

                    val soundUri: Uri = Settings.System.DEFAULT_NOTIFICATION_URI
                    val attrs =
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    setSound(soundUri, attrs)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val contentIntent =
            PendingIntent.getActivity(
                context,
                name.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val notificationId = name.hashCode()

        val muteIntent =
            Intent(context, CourseReminderReceiver::class.java).apply {
                action = ACTION_MUTE_DEVICE
                putExtra("notification_id", notificationId)
                putExtra(EXTRA_COURSE_START_TIME, time)
                putExtra(EXTRA_COURSE_END_TIME, endTime)
            }
        val mutePendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val smallView =
            RemoteViews(context.packageName, R.layout.small_notify).apply {
                val locStr = location.ifBlank { "无地点信息" }
                setTextViewText(R.id.notification_title, name)
                setTextViewText(R.id.notification_content, "地点:$locStr\n还有15分钟上课")
                setOnClickPendingIntent(R.id.btn_mute_small, mutePendingIntent)
            }

        val largeView =
            RemoteViews(context.packageName, R.layout.large_notify).apply {
                setTextViewText(R.id.notification_title, name)
                val timeStr = if (time.isNotBlank()) "时间：$time\n" else ""
                val locStr = if (location.isNotBlank()) "地点：$location\n" else ""
                setTextViewText(
                    R.id.notification_body,
                    "${locStr}${timeStr}距离上课还有不到15分钟\n" + "点击右侧按钮可开启静音"
                )
                setOnClickPendingIntent(R.id.btn_mute_large, mutePendingIntent)
            }

        val notification =
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(name)
                .setContentText("距离上课还有不到15分钟")
                .setCustomContentView(smallView)
                .setCustomBigContentView(largeView)
                .setCustomHeadsUpContentView(smallView)
                .setStyle(Notification.DecoratedCustomViewStyle())
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(notificationId, notification)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val deps = Deps()
                deps.rescheduleNextReminderIfEnabled()
            } catch (_: Exception) {

            } finally {
                result.finish()
            }
        }
    }

    private class Deps : KoinComponent {
        val rescheduleNextReminderIfEnabled: RescheduleNextCourseReminderIfEnabledUseCase by inject()
    }

    private fun scheduleClassMute(
        context: Context,
        notificationId: Int,
        startAtMs: Long,
        endTime: String,
        restoreMode: Int
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val muteIntent =
            Intent(context, CourseReminderReceiver::class.java).apply {
                action = ACTION_APPLY_CLASS_MUTE
                putExtra("notification_id", notificationId)
                putExtra(EXTRA_COURSE_END_TIME, endTime)
                putExtra(EXTRA_RESTORE_RINGER_MODE, restoreMode)
            }
        val requestCode = notificationId xor MUTE_REQUEST_CODE_MASK
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startAtMs, pendingIntent)
    }

    private fun scheduleAutoUnmute(
        context: Context,
        notificationId: Int,
        endTime: String,
        restoreMode: Int
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMs = parseTodayTimeMs(endTime)

        val unmuteIntent =
            Intent(context, CourseReminderReceiver::class.java).apply {
                action = ACTION_UNMUTE_DEVICE
                putExtra(EXTRA_RESTORE_RINGER_MODE, restoreMode)
            }
        val requestCode = notificationId xor UNMUTE_REQUEST_CODE_MASK
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                unmuteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val now = System.currentTimeMillis()
        val safeTrigger =
            when {
                triggerAtMs == null -> now + FALLBACK_UNMUTE_DELAY_MS
                triggerAtMs <= now -> now + 5000L
                else -> triggerAtMs
            }

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
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
        const val ACTION_MUTE_DEVICE = "restarhalf.stellar.schedule.ACTION_MUTE_DEVICE"
        const val ACTION_APPLY_CLASS_MUTE = "restarhalf.stellar.schedule.ACTION_APPLY_CLASS_MUTE"
        const val ACTION_UNMUTE_DEVICE = "restarhalf.stellar.schedule.ACTION_UNMUTE_DEVICE"

        private const val EXTRA_COURSE_START_TIME = "course_time"
        private const val EXTRA_COURSE_END_TIME = "course_end_time"
        private const val EXTRA_RESTORE_RINGER_MODE = "restore_ringer_mode"
        private const val FALLBACK_UNMUTE_DELAY_MS = 90 * 60 * 1000L
        private const val MUTE_REQUEST_CODE_MASK = 0x4D2A
        private const val UNMUTE_REQUEST_CODE_MASK = 0x1F3A
    }
}
