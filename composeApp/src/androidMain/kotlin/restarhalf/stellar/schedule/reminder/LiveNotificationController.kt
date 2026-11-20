package restarhalf.stellar.schedule.reminder

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import com.russhwolf.settings.ObservableSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.MainActivity
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.reminder.ClassroomLabel
import restarhalf.stellar.schedule.reminder.receiver.ClassDndReceiver
import restarhalf.stellar.schedule.reminder.receiver.CourseStartReceiver
import restarhalf.stellar.schedule.reminder.receiver.ExamDndReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 原生实况通知，照 Nexio 的 showPreClassCountdownNotification / updateActiveCountdown /
 * showStartedLiveNotification / showOrUpdateInClassLiveNotification：
 *
 * - 三态固定 ID（倒计时/已开始/进行中），与超级岛同思路，按 [ReminderSession] 分段；
 * - 只在没有超级岛能力（或用户关了岛）时使用，岛可用时由 IslandNotificationHelper 接管；
 * - 到点由精确闹钟 [CourseStartReceiver] 触发切换，每分钟由对账链补推；
 * - 进行中进度 API 36+ 走 ProgressStyle + 提升 ongoing，低版本降级为普通进度条。
 *
 * 课程与考试共用整条链路，仅 ID/文案/开关不同。
 */
internal object LiveNotificationController : KoinComponent {

    const val LIVE_COUNTDOWN_ID = 2003
    const val LIVE_STARTED_ID = 2004
    const val LIVE_IN_CLASS_ID = 2005

    /** 测试实时活动独立 ID，对齐超级岛测试 5000/5001/5002，不覆盖真实课提醒 */
    const val LIVE_TEST_COUNTDOWN_ID = 5100
    const val LIVE_TEST_STARTED_ID = 5101
    const val LIVE_TEST_IN_CLASS_ID = 5102

    private fun prefs(): ObservableSettings = get(named(SettingsKeys.PREFS_NAME))

    /** 岛优先：岛开着且有岛能力时，实况通知整体让位。 */
    fun isIslandMode(context: Context): Boolean =
        IslandNotificationHelper.isEnabled(context) && IslandNotificationHelper.isSupported(context)

    // ————————————————————————— 课前/考前倒计时 —————————————————————————

    fun showPreClassCountdownNotification(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String,
        startMillis: Long,
        endMillis: Long,
        session: ReminderSession = ReminderSession.Course,
    ) {
        if (isIslandMode(context)) return

        NotificationChannels.ensureLive(context)
        val now = System.currentTimeMillis()
        if (startMillis <= now) return

        val labels = session.labels
        val contentIntent = openAppIntent(context, courseName.hashCode())
        val dndIntent = dndToggleIntent(context, courseName.hashCode() + 100, session)

        val minutesUntilStart = ceilMinutesUntil(startMillis, now)
        val collapsedMode = prefs().getInt(SettingsKeys.LIVE_RIGHT_MODE, 0)
        val labelRule = ClassroomLabel.read(prefs())
        // 缩略态只有十几个半角宽，名称和地点都先按用户规则精简一手
        val shortCourse = ClassroomLabel.applyCourse(courseName, labelRule).ifEmpty { courseName }
        val shortClassroom = ClassroomLabel.apply(classroom, labelRule).ifEmpty { courseName }
        val shortCriticalText =
            when (collapsedMode) {
                1 -> shortClassroom
                2 -> minutesShort(minutesUntilStart)
                else -> shortCourse
            }

        val bigText =
            buildPreClassBigText(
                minutesUntilStart = minutesUntilStart,
                startTime = startTime,
                endTime = formatClock(endMillis),
                classroom = classroom,
                labels = labels,
            )

        val builder =
            Notification.Builder(context, NotificationChannels.LIVE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(courseName)
                .setContentText(bigText.replace('\n', ' '))
                .setStyle(Notification.BigTextStyle().bigText(bigText))
                .setShowWhen(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .setCategory(Notification.CATEGORY_REMINDER)
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.mipmap.ic_launcher),
                        labels.viewAction,
                        contentIntent,
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_live_dot),
                        labels.dndAction,
                        dndIntent,
                    ).build()
                )
                .apply {
                    val timeout = endMillis - now
                    if (timeout > 0) setTimeoutAfter(timeout)
                }
        applyLivePromotion(builder, shortCriticalText)

        val notification = builder.build().apply { flags = flags or Notification.FLAG_ONLY_ALERT_ONCE }

        context.getSystemService(NotificationManager::class.java)
            ?.notify(session.liveCountdownId, notification)

        LiveReminderState.saveCountdown(
            courseName = courseName,
            classroom = classroom,
            section = "",
            startTime = startTime,
            startMillis = startMillis,
            endMillis = endMillis,
            session = session,
        )
        LiveReminderState.setLastDisplayedMinutes(minutesUntilStart, session)

        // 到点精确切换，不依赖分钟链的触发时点
        scheduleCourseStartAlarm(context, startMillis, session)
    }

    /**
     * 测试实时活动，照 Nexio 的 sendTestLiveNotification：
     * 固定测试课（70 秒后上课、课中 2 分钟），独立 ID 与独立状态键，
     * 并注册测试课的勿扰起止闹钟。
     */
    suspend fun sendTestLiveNotification(context: Context) {
        val session = ReminderSession.CourseTest
        val startMillis = System.currentTimeMillis() + IslandNotificationHelper.TEST_COUNTDOWN_MS
        val endMillis = startMillis + IslandNotificationHelper.TEST_CLASS_DURATION_MS
        showPreClassCountdownNotification(
            context = context,
            courseName = IslandNotificationHelper.TEST_COURSE_NAME,
            classroom = IslandNotificationHelper.TEST_CLASSROOM,
            startTime = formatClock(startMillis),
            startMillis = startMillis,
            endMillis = endMillis,
            session = session,
        )
        // 测试课也要走「上课自动开启、下课自动关闭」，否则勿扰链路在测试里跑不到
        // 窗口直接由测试倒计时状态派生，见 LiveReminderState.testClassWindow
        CourseDayAlarmScheduler.syncTestClassDndAlarms(context)
        ReminderTickScheduler.scheduleNext(context)
    }

    /** 到点后由 [CourseStartReceiver] / 对账链驱动：进行中进度优先，否则闪一次「已上课/已开考」。 */
    fun updateActiveCountdown(context: Context) {
        // 真实课程/考试与测试课都要扫，否则倒计时/进行中进度永远不推进
        updateActiveCountdown(context, ReminderSession.Course)
        updateActiveCountdown(context, ReminderSession.Exam)
        updateActiveCountdown(context, ReminderSession.CourseTest)
    }

    private fun updateActiveCountdown(context: Context, session: ReminderSession) {
        if (!LiveReminderState.isCountdownActive(session)) return

        if (isIslandMode(context)) {
            // 岛模式下不显示实况通知：本会话三态收起，改由岛状态对账接管
            cancelLiveNotifications(context, session)
            LiveReminderState.clearCountdown(session)
            IslandNotificationHelper.reconcileIslandCountdown(context)
            return
        }

        val labels = session.labels
        val state = LiveReminderState.countdown(session) ?: return
        val manager = context.getSystemService(NotificationManager::class.java)
        val now = System.currentTimeMillis()

        // 结束：三态全部收起并清状态
        if (state.endMillis in 1..now) {
            cancelLiveNotifications(context, session)
            LiveReminderState.clearCountdown(session)
            IslandNotificationHelper.cancelIslandState(context, session.islandCountdownId)
            return
        }

        if (now >= state.startMillis) {
            val inClassOn =
                state.endMillis > now &&
                    IslandNotificationHelper.shouldShowInClassNow(
                        context,
                        state.startMillis,
                        state.endMillis,
                        now,
                        session,
                    )
            if (inClassOn) {
                // 进行中：保持 active，挂到结束由对账链继续驱动
                if (!state.inClassActive) {
                    manager?.cancel(session.liveCountdownId)
                    IslandNotificationHelper.cancelIslandState(context, session.islandCountdownId)
                }
                showOrUpdateInClassLiveNotification(
                    context = context,
                    courseName = state.courseName,
                    classroom = state.classroom,
                    endTime = formatClock(state.endMillis),
                    startMillis = state.startMillis,
                    endMillis = state.endMillis,
                    session = session,
                )
                return
            }

            if (state.endMillis > now && isInClassEnabled(context, session)) {
                // 进行中已开但「距结束」未进窗：不发「已开始」，收起倒计时等进窗再切进行中
                manager?.cancel(session.liveCountdownId)
                IslandNotificationHelper.cancelIslandState(context, session.islandCountdownId)
                return
            }

            // 进行中未开：到点闪一次「已上课/已开考」
            manager?.cancel(session.liveCountdownId)
            IslandNotificationHelper.cancelIslandState(context, session.islandCountdownId)
            manager?.cancel(session.liveInClassId)
            LiveReminderState.clearCountdown(session)
            showStartedLiveNotification(
                context = context,
                courseName = state.courseName,
                startTime = state.startTime,
                session = session,
            )
            return
        }

        // 课前/考前每分钟推进；只在分钟数变化时重建，避免岛/胶囊闪烁
        val minutesUntilStart = ceilMinutesUntil(state.startMillis, now)
        if (minutesUntilStart == state.lastDisplayedMinutes) return

        val collapsedMode = prefs().getInt(SettingsKeys.LIVE_RIGHT_MODE, 0)
        val labelRule = ClassroomLabel.read(prefs())
        val shortCourse = ClassroomLabel.applyCourse(state.courseName, labelRule)
            .ifEmpty { state.courseName }
        val shortClassroom = ClassroomLabel.apply(state.classroom, labelRule)
            .ifEmpty { state.courseName }
        val shortCriticalText =
            when (collapsedMode) {
                1 -> shortClassroom
                2 -> minutesShort(minutesUntilStart)
                else -> shortCourse
            }

        val bigText =
            buildPreClassBigText(
                minutesUntilStart = minutesUntilStart,
                startTime = state.startTime,
                endTime = formatClock(state.endMillis),
                classroom = state.classroom,
                labels = labels,
            )

        val contentIntent = openAppIntent(context, state.courseName.hashCode())
        val dndIntent = dndToggleIntent(context, state.courseName.hashCode() + 100, session)

        val builder =
            Notification.Builder(context, NotificationChannels.LIVE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(state.courseName)
                .setContentText(bigText.replace('\n', ' '))
                .setStyle(Notification.BigTextStyle().bigText(bigText))
                .setShowWhen(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .setCategory(Notification.CATEGORY_REMINDER)
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.mipmap.ic_launcher),
                        labels.viewAction,
                        contentIntent,
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_live_dot),
                        labels.dndAction,
                        dndIntent,
                    ).build()
                )
                .apply {
                    val timeout = state.endMillis - now
                    if (timeout > 0) setTimeoutAfter(timeout)
                }
        applyLivePromotion(builder, shortCriticalText)

        val notification = builder.build().apply { flags = flags or Notification.FLAG_ONLY_ALERT_ONCE }

        manager?.notify(session.liveCountdownId, notification)
        LiveReminderState.setLastDisplayedMinutes(minutesUntilStart, session)
    }

    // ————————————————————————— 已上课/已开考 —————————————————————————

    /** 15 秒「已上课/已开考」实况；独立 ID，与倒计时/进行中错开。 */
    fun showStartedLiveNotification(
        context: Context,
        courseName: String,
        startTime: String,
        session: ReminderSession = ReminderSession.Course,
    ) {
        if (isIslandMode(context)) return

        NotificationChannels.ensureLive(context)
        val labels = session.labels
        val startedIntent = openAppIntent(context, courseName.hashCode())
        val dndIntent = dndToggleIntent(context, courseName.hashCode() + 100, session)

        val bigText =
            buildString {
                if (startTime.isNotEmpty()) {
                    append(startTime)
                    append(" · ")
                }
                append(labels.nowSession)
            }

        val builder =
            Notification.Builder(context, NotificationChannels.LIVE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(courseName)
                .setContentText(bigText)
                .setStyle(Notification.BigTextStyle().bigText(bigText))
                .setOngoing(true)
                .setContentIntent(startedIntent)
                .setCategory(Notification.CATEGORY_REMINDER)
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.mipmap.ic_launcher),
                        labels.viewAction,
                        startedIntent,
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_live_dot),
                        labels.dndAction,
                        dndIntent,
                    ).build()
                )
                .setTimeoutAfter(STARTED_VISIBLE_MS)
        applyLivePromotion(builder, courseName)

        val notification = builder.build().apply { flags = flags or Notification.FLAG_ONLY_ALERT_ONCE }

        context.getSystemService(NotificationManager::class.java)
            ?.notify(session.liveStartedId, notification)
    }

    // ————————————————————————— 进行中进度 —————————————————————————

    /**
     * API 36+ 用 ProgressStyle + 提升 ongoing，与 SleepDown 同思路。
     * 每分钟只在剩余分钟/进度变化时重推，挂到下课/考试结束自动结束。
     */
    @SuppressLint("NewApi")
    fun showOrUpdateInClassLiveNotification(
        context: Context,
        courseName: String,
        classroom: String,
        endTime: String,
        startMillis: Long,
        endMillis: Long,
        session: ReminderSession = ReminderSession.Course,
    ) {
        if (isIslandMode(context)) return

        NotificationChannels.ensureLive(context)
        val now = System.currentTimeMillis()
        if (endMillis <= now) return

        val labels = session.labels
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val state = LiveReminderState.countdown(session)
        val wasInClass = state?.inClassActive == true

        val remainMin = inClassRemainMinutes(endMillis, now)
        val progress = inClassProgressPercent(startMillis, endMillis, remainMin)
        val lastMin = state?.lastInClassMinutes ?: -1
        val lastProgress = state?.lastInClassProgress ?: -1

        // 首次切入进行中：收起倒计时/已开始，避免三态并存
        if (!wasInClass) {
            manager.cancel(session.liveCountdownId)
            manager.cancel(session.liveStartedId)
        } else if (remainMin == lastMin && progress == lastProgress) {
            // 同分钟无变化，不重推，避免岛/胶囊闪烁
            return
        }

        val contentIntent = openAppIntent(context, courseName.hashCode())
        val infoLine =
            if (endTime.isNotEmpty()) {
                "$endTime${labels.endsAtSuffix} · $REMAINING_PREFIX${remainMin}$MINUTES_SUFFIX"
            } else {
                "${labels.distanceToEnd} · $REMAINING_PREFIX${remainMin}$MINUTES_SUFFIX"
            }
        val expandedText = if (classroom.isBlank()) infoLine else "$infoLine\n$classroom"
        val shortCriticalText = minutesShort(remainMin)

        val notification =
            if (Build.VERSION.SDK_INT >= 36) {
                val builder =
                    Notification.Builder(context, NotificationChannels.LIVE_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(courseName)
                        .setContentText(infoLine)
                        .setStyle(Notification.BigTextStyle().bigText(expandedText))
                        .setContentIntent(contentIntent)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setShowWhen(false)
                        .setCategory(Notification.CATEGORY_PROGRESS)
                        .setTimeoutAfter(endMillis - now)
                val progressStyle =
                    Notification.ProgressStyle()
                        .setProgressSegments(listOf(Notification.ProgressStyle.Segment(100)))
                        .setProgress(progress)
                // ColorOS 优化会把自定义标头挤偏，走系统默认 tracker
                if (!isColorOs()) {
                    progressStyle.setProgressTrackerIcon(
                        Icon.createWithResource(context, R.drawable.ic_live_dot)
                    )
                }
                builder.setStyle(progressStyle)
                runCatching { builder.setRequestPromotedOngoing(true) }
                runCatching { builder.setShortCriticalText(shortCriticalText) }
                builder.build().apply { flags = flags or Notification.FLAG_ONLY_ALERT_ONCE }
            } else {
                // 低版本无 ProgressStyle：用普通进度条降级
                Notification.Builder(context, NotificationChannels.LIVE_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(courseName)
                    .setContentText(expandedText.replace('\n', ' '))
                    .setStyle(Notification.BigTextStyle().bigText(expandedText))
                    .setProgress(100, progress, false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setContentIntent(contentIntent)
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setTimeoutAfter(endMillis - now)
                    .build()
                    .apply { flags = flags or Notification.FLAG_ONLY_ALERT_ONCE }
            }

        manager.notify(session.liveInClassId, notification)
        LiveReminderState.setInClass(
            active = true,
            minutes = remainMin,
            progress = progress,
            session = session,
        )
    }

    // ————————————————————————— 收起 —————————————————————————

    /** 取消课程侧三态通知并清倒计时状态（含测试课），不动岛状态、不动考试侧。 */
    fun cancelAll(context: Context) {
        cancelSession(context, ReminderSession.Course)
        cancelSession(context, ReminderSession.CourseTest)
    }

    /** 收掉某会话的实况三态并清状态、取消到点切换闹钟。 */
    fun cancelSession(context: Context, session: ReminderSession) {
        cancelLiveNotifications(context, session)
        ReminderAlarmScheduler.cancel(
            context,
            session.liveCountdownId,
            CourseStartReceiver::class.java,
        )
        LiveReminderState.clearCountdown(session)
    }

    private fun cancelLiveNotifications(context: Context, session: ReminderSession) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(session.liveCountdownId)
        manager?.cancel(session.liveStartedId)
        manager?.cancel(session.liveInClassId)
    }

    // ————————————————————————— 工具 —————————————————————————

    private fun scheduleCourseStartAlarm(
        context: Context,
        startMillis: Long,
        session: ReminderSession = ReminderSession.Course,
    ) {
        ReminderAlarmScheduler.scheduleBoundary(
            context = context,
            triggerAtMs = startMillis + COURSE_START_OFFSET_MS,
            pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    session.liveCountdownId,
                    Intent(context, CourseStartReceiver::class.java).apply {
                        putExtra(EXTRA_REMINDER_SESSION, session.name)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
        )
    }

    private fun openAppIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun dndToggleIntent(
        context: Context,
        requestCode: Int,
        session: ReminderSession,
    ): PendingIntent {
        val receiver =
            if (session.isExam) ExamDndReceiver::class.java else ClassDndReceiver::class.java
        val action =
            if (session.isExam) ExamDndReceiver.ACTION_TOGGLE else ClassDndReceiver.ACTION_TOGGLE
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, receiver).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun isInClassEnabled(context: Context, session: ReminderSession): Boolean =
        prefs().getBoolean(SettingsKeys.REMINDER_ENABLED, false) &&
            prefs().getBoolean(session.settingsKeys.inSessionEnabled, false)

    // 向上取整，保证文案与系统倒计时剩余秒数一致
    private fun ceilMinutesUntil(startMillis: Long, now: Long = System.currentTimeMillis()): Int {
        val remain = startMillis - now
        if (remain <= 0) return 0
        return ((remain + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    }

    private fun formatClock(millis: Long): String {
        if (millis <= 0L) return ""
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    }

    /** 课前/考前实况正文：第一行「N分钟后上课/开考 · 时间 - 时间」，第二行地点。 */
    private fun buildPreClassBigText(
        minutesUntilStart: Int,
        startTime: String,
        endTime: String,
        classroom: String,
        labels: ReminderLabels,
    ): String =
        buildString {
            val timeRange =
                when {
                    startTime.isNotEmpty() && endTime.isNotEmpty() -> "$startTime - $endTime"
                    startTime.isNotEmpty() -> startTime
                    endTime.isNotEmpty() -> endTime
                    else -> ""
                }
            append(
                if (timeRange.isEmpty()) {
                    "$minutesUntilStart${labels.preClassSuffix}"
                } else {
                    "$minutesUntilStart${labels.preClassSuffix} · $timeRange"
                }
            )
            if (classroom.isNotEmpty()) {
                append('\n')
                append(classroom)
            }
        }

    // 与超级岛同口径：剩余分钟向上取整；进度按已进行比例（0→100）
    private fun inClassRemainMinutes(endMillis: Long, now: Long): Int {
        val remainMs = endMillis - now
        if (remainMs <= 0L) return 0
        return ((remainMs + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
    }

    private fun inClassProgressPercent(
        startMillis: Long,
        endMillis: Long,
        remainMinutes: Int,
    ): Int {
        val totalMs = endMillis - startMillis
        if (totalMs <= 0L) return 0
        val remainMs = remainMinutes * 60_000L
        val elapsedMs = (totalMs - remainMs).coerceIn(0L, totalMs)
        return ((elapsedMs * 100L) / totalMs).toInt().coerceIn(0, 100)
    }

    /** ColorOS（OPPO/realme/OnePlus）会优化进度条标头导致偏移，进行中进度用系统默认 tracker。 */
    private fun isColorOs(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        val colorOsBrands = setOf("oppo", "realme", "oneplus")
        if (manufacturer in colorOsBrands || brand in colorOsBrands) return true
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val get = clazz.getMethod("get", String::class.java)
            val rom = (get.invoke(null, "ro.build.version.oplusrom") as? String).orEmpty()
            rom.isNotBlank()
        }.getOrDefault(false)
    }

    private fun minutesShort(minutes: Int): String = "$minutes$MINUTES_UNIT"

    /** API 36+：请求提升为实况/常驻卡，并把缩略态文案交给系统。 */
    private fun applyLivePromotion(builder: Notification.Builder, shortText: String) {
        if (Build.VERSION.SDK_INT < 36) return
        runCatching { builder.setRequestPromotedOngoing(true) }
        if (shortText.isNotEmpty()) {
            runCatching { builder.setShortCriticalText(shortText) }
        }
    }

    private const val COURSE_START_OFFSET_MS = 100L
    private const val STARTED_VISIBLE_MS = 15_000L

    private const val REMAINING_PREFIX = "还剩"
    private const val MINUTES_SUFFIX = "分钟"
    private const val MINUTES_UNIT = "分钟"
}