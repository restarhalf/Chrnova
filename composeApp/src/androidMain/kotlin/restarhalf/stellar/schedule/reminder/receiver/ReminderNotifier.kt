package restarhalf.stellar.schedule.reminder.receiver

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import restarhalf.stellar.schedule.MainActivity
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.reminder.ReminderKind

internal data class ReminderPresentation(
    val kind: ReminderKind,
    val notificationId: Int,
    val channelId: String,
    val category: String,
    val cardTitle: String,
    val contentTitle: String,
    val contentText: String,
    val startText: String,
    val endText: String,
    val detailTitle: String,
    val detailText: String,
    val placeLabel: String,
    val muteLabel: String,
    val timeoutMs: Long,
    val timeRange: String,
    val statusLabel: String,
    val timerType: Int,
    val timerWhenMs: Long,
    val ticker: String,
    val shareContent: String,
)

internal object ReminderNotifier {

    private const val MUTE_BUTTON_LABEL = "静音"

    @RequiresApi(Build.VERSION_CODES.O)
    fun notify(context: Context, presentation: ReminderPresentation, nowMs: Long) {
        val contentIntent =
            PendingIntent.getActivity(
                context,
                presentation.notificationId,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val mutePendingIntent =
            PendingIntent.getActivity(
                context,
                presentation.notificationId,
                MuteActionActivity.muteIntent(
                    context = context,
                    kind = presentation.kind,
                    notificationId = presentation.notificationId,
                    startTime = presentation.startText,
                    endTime = presentation.endText,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val largeView =
            RemoteViews(context.packageName, R.layout.notify_reminder_large).apply {
                setTextViewText(R.id.notify_title, presentation.cardTitle)
                setTextViewText(R.id.notify_start_time, presentation.startText)
                setTextViewText(R.id.notify_end_time, presentation.endText)
                setTextViewText(R.id.notify_detail_title, presentation.detailTitle)
                setTextViewText(R.id.notify_detail_desc, presentation.detailText)
                setTextViewText(R.id.notify_button_text, MUTE_BUTTON_LABEL)
                setOnClickPendingIntent(R.id.notify_button_layout, mutePendingIntent)
                setOnClickPendingIntent(R.id.notify_root, contentIntent)
            }

        val muteAction =
            Notification.Action.Builder(
                null,
                presentation.muteLabel,
                mutePendingIntent,
            ).build()

        val notification =
            Notification.Builder(context, presentation.channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(presentation.contentTitle)
                .setContentText(presentation.contentText)
                .setCustomContentView(largeView)
                .setCustomBigContentView(largeView)
                .setCustomHeadsUpContentView(largeView)
                .setCategory(presentation.category)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(presentation.timeoutMs)
                .addAction(muteAction)
                .build()
        notification.extras.putBoolean("miui.customHeight", true)
        notification.extras.putBoolean("miui.showAction", true)

        FocusNotification.attach(
            context = context,
            notification = notification,
            content =
                FocusContent(
                    business = presentation.kind.business,
                    courseName = presentation.detailTitle,
                    timeRange = presentation.timeRange,
                    statusLabel = presentation.statusLabel,
                    timerType = presentation.timerType,
                    timerWhenMs = presentation.timerWhenMs,
                    placeLabel = presentation.placeLabel,
                    placeText = presentation.detailText,
                    ticker = presentation.ticker,
                    shareContent = presentation.shareContent,
                    muteAction = muteAction,
                    muteTitle = presentation.muteLabel,
                ),
            nowMs = nowMs,
        )

        context.getSystemService(NotificationManager::class.java)
            ?.notify(presentation.notificationId, notification)
    }
}
