package restarhalf.stellar.schedule.reminder

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import com.russhwolf.settings.ObservableSettings
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.MainActivity
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.reminder.ClassroomLabel
import restarhalf.stellar.schedule.reminder.receiver.ExamDndReceiver
import restarhalf.stellar.schedule.reminder.receiver.ClassDndReceiver
import restarhalf.stellar.schedule.reminder.receiver.FocusCapabilities
import restarhalf.stellar.schedule.reminder.receiver.FocusTier
import restarhalf.stellar.schedule.reminder.receiver.IslandDismissReceiver
import restarhalf.stellar.schedule.shizuku.ShizukuManager
import restarhalf.stellar.schedule.reminder.receiver.IslandExpandReceiver
import java.util.concurrent.atomic.AtomicLong

/**
 * 超级岛通知，照 Nexio 的 IslandNotificationHelper：
 * 课前倒计时（模板 2 + 系统 timer）、到点「已上课/已开考」、进行中进度（模板 2，A 区名称、
 * B 区静态「正在上课/正在考试」或距结束倒计时），三态各用独立通知 ID，避免同 ID 更新被岛框架
 * 当成静默替换而卡在 00:00。课程与考试（[ReminderSession]）共用整条链路，只换 ID/文案/开关。
 *
 * 发岛前经 Shizuku 临时掐掉 XMSF 网络（照 Nexio 的 withShizukuBypass），没授权就退化为普通 notify；
 * 岛能力检测沿用 FocusCapabilities。
 */
internal object IslandNotificationHelper : KoinComponent {

    const val ISLAND_NOTIFICATION_ID = 1003
    const val ISLAND_STARTED_NOTIFICATION_ID = 1004
    const val ISLAND_IN_CLASS_NOTIFICATION_ID = 1005

    /** 测试课用独立 ID（照 Nexio 的 5000/5001/5002），不碰真实提醒三态。 */
    const val ISLAND_TEST_NOTIFICATION_ID = 5000
    const val ISLAND_STARTED_TEST_NOTIFICATION_ID = 5001
    const val ISLAND_IN_CLASS_TEST_NOTIFICATION_ID = 5002

    /** 到点后「已上课」只留 15 秒，由精确闹钟收起。 */
    const val ISLAND_STARTED_VISIBLE_MS = 15_000L

    /** 测试课的固定文案与节奏，照 Nexio：70 秒后上课、课中 2 分钟。 */
    const val TEST_COURSE_NAME = "大学英语Ⅱ"
    const val TEST_CLASSROOM = "博A201"
    const val TEST_COUNTDOWN_MS = 70_000L
    const val TEST_CLASS_DURATION_MS = 120_000L

    private val sequenceCounter = AtomicLong(System.currentTimeMillis() / 1000)

    private val shizukuBypassLock = java.util.concurrent.locks.ReentrantLock()

    private fun prefs(): ObservableSettings = get(named(SettingsKeys.PREFS_NAME))

    fun isSupported(context: Context): Boolean =
        FocusCapabilities.resolve(context) == FocusTier.ISLAND

    fun isEnabled(context: Context): Boolean =
        prefs().getBoolean(SettingsKeys.ISLAND_NOTIFICATION_ENABLED, true)

    /** 课中/考试中提醒开关。 */
    fun isInClassEnabled(context: Context, session: ReminderSession = ReminderSession.Course): Boolean {
        val keys = session.settingsKeys
        val p = prefs()
        return p.getBoolean(SettingsKeys.REMINDER_ENABLED, false) &&
            p.getBoolean(keys.inSessionEnabled, false)
    }

    /**
     * 当前是否应展示课中/考试中进度。
     * 距下课/距结束模式：剩余时长 <= N 分钟才展示；N 不小于本场总时长时按全程。
     */
    fun shouldShowInClassNow(
        context: Context,
        startMillis: Long,
        endMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        session: ReminderSession = ReminderSession.Course,
    ): Boolean {
        if (!isInClassEnabled(context, session)) return false
        if (endMillis <= startMillis) return false
        if (nowMillis < startMillis || nowMillis >= endMillis) return false
        val p = prefs()
        val keys = session.settingsKeys
        if (p.getInt(keys.inSessionTimingMode, 0) == 0) return true
        val leadMs = p.getInt(keys.inSessionLeadMinutes, 10).coerceIn(0, 60) * 60_000L
        val totalMs = endMillis - startMillis
        if (leadMs >= totalMs) return true
        return (endMillis - nowMillis) <= leadMs
    }

    // ————————————————————————— 发送 —————————————————————————

    fun sendPreClassIslandNotification(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String,
        startMillis: Long,
        endMillis: Long,
        session: ReminderSession = ReminderSession.Course,
    ) {
        val title = if (startTime.isNotEmpty()) "$courseName $startTime" else courseName
        val content = buildIslandContent(classroom)
        val countdownId = session.islandCountdownId

        if (startMillis <= System.currentTimeMillis()) {
            LiveReminderState.saveIsland(
                notificationId = countdownId,
                courseName = courseName,
                classroom = classroom,
                section = "",
                startTime = startTime,
                endTime = formatClock(endMillis),
                startMillis = startMillis,
                endMillis = endMillis,
                session = session,
            )
            onClassStart(
                context = context,
                courseName = courseName,
                classroom = classroom,
                startTime = startTime,
                endTime = formatClock(endMillis),
                notificationId = countdownId,
                session = session,
            )
            return
        }

        // 上一场残留的「已上课/已开考 / 进行中」岛先收起，避免与本次倒计时岛并存
        cancelIslandState(context, session.islandStartedId)
        cancelIslandState(context, session.islandInClassId)

        LiveReminderState.saveIsland(
            notificationId = countdownId,
            courseName = courseName,
            classroom = classroom,
            section = "",
            startTime = startTime,
            endTime = formatClock(endMillis),
            startMillis = startMillis,
            endMillis = endMillis,
            session = session,
        )

        sendIslandNotification(
            context = context,
            notificationId = countdownId,
            title = title,
            content = content,
            courseName = courseName,
            startTime = startTime,
            classroom = classroom,
            courseStartMillis = startMillis,
            session = session,
        )
    }

    /**
     * 到点分流：开了课中/考试中提醒且已进窗 → 进行中卡；未进窗 → 收倒计时等对账；
     * 未开 → 静态「已上课/已开考」15 秒。
     */
    fun onClassStart(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String,
        endTime: String?,
        notificationId: Int,
        session: ReminderSession = ReminderSession.Course,
    ) {
        val state = LiveReminderState.islandSnapshot(session)
        val startMillis = state?.startMillis ?: System.currentTimeMillis()
        val endMillis = state?.endMillis ?: 0L

        if (isInClassEnabled(context, session)) {
            val showNow =
                endMillis > startMillis &&
                    shouldShowInClassNow(context, startMillis, endMillis, session = session)
            if (showNow) {
                sendInClassIslandNotification(
                    context = context,
                    courseName = courseName,
                    classroom = classroom,
                    startTime = startTime,
                    endTime = endTime ?: "",
                    notificationId = notificationId,
                    session = session,
                )
            } else {
                // 「距下课/距结束」未进窗：只收倒计时，不发已上课，标记 switched 交给对账
                val manager = context.getSystemService(NotificationManager::class.java)
                manager?.cancel(countdownIdFor(notificationId))
                LiveReminderState.markIslandSwitched(session)
            }
            return
        }

        sendClassStartedNotification(
            context = context,
            courseName = courseName,
            classroom = classroom,
            startTime = startTime,
            endTime = endTime,
            notificationId = notificationId,
            session = session,
        )
    }

    /** 静态「已上课/已开考」：独立 ID，15 秒后收起。 */
    fun sendClassStartedNotification(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String,
        endTime: String?,
        notificationId: Int,
        session: ReminderSession = ReminderSession.Course,
    ) {
        if (!isSupported(context)) return

        val startedId = activeIdFor(notificationId, inClass = false, session = session)
        val snapshot = LiveReminderState.islandSnapshot(session)
        if (snapshot?.notificationId == startedId && snapshot.switched) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(countdownIdFor(notificationId))
        manager?.cancel(activeIdFor(notificationId, inClass = true, session = session))

        val title = if (startTime.isNotEmpty()) "$courseName $startTime" else courseName
        sendIslandNotification(
            context = context,
            notificationId = startedId,
            title = title,
            content = buildIslandContent(classroom),
            courseName = courseName,
            startTime = startTime,
            endTime = endTime,
            classroom = classroom,
            courseStartMillis = null,
            session = session,
        )

        val startMillis = snapshot?.startMillis ?: -1L
        LiveReminderState.updateIslandNotificationId(startedId, session)
        LiveReminderState.markIslandSwitched(session)
        scheduleIslandDismiss(context, startedId, startMillis, session)
    }

    /** 进行中：A 区名称，B 区「正在上课/正在考试」或距结束倒计时，挂到下课/考试结束。 */
    fun sendInClassIslandNotification(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String,
        endTime: String,
        notificationId: Int,
        session: ReminderSession = ReminderSession.Course,
    ) {
        if (!isSupported(context)) return

        val inClassId = activeIdFor(notificationId, inClass = true, session = session)
        val state = LiveReminderState.islandSnapshot(session)
        val startMillis = state?.startMillis ?: System.currentTimeMillis()
        val endMillis = state?.endMillis ?: 0L
        val now = System.currentTimeMillis()
        if (endMillis <= now) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(countdownIdFor(notificationId))
        manager?.cancel(activeIdFor(notificationId, inClass = false, session = session))

        val params =
            buildInClassIslandParamsJson(
                context = context,
                courseName = courseName,
                classroom = classroom,
                startTime = startTime,
                endTime = endTime,
                courseEndMillis = endMillis,
                session = session,
            )

        sendIslandNotification(
            context = context,
            notificationId = inClassId,
            title = if (startTime.isNotEmpty()) "$courseName $startTime" else courseName,
            content =
                buildString {
                    if (classroom.isNotEmpty()) append(classroom)
                    if (isNotEmpty()) append(" ")
                    if (endTime.isNotEmpty()) append("$endTime${session.labels.endsAtSuffix}")
                },
            courseName = courseName,
            startTime = startTime,
            endTime = endTime,
            classroom = classroom,
            islandParamsOverride = params,
            session = session,
        )

        LiveReminderState.updateIslandNotificationId(inClassId, session)
        LiveReminderState.markIslandSwitched(session)
        scheduleIslandDismissAt(context, inClassId, startMillis, endMillis, session)
    }

    /**
     * 测试小米超级岛，照 Nexio 的 sendTestIslandNotification：
     * 固定测试课（70 秒后上课、课中 2 分钟），走独立 ID 与独立状态键，
     * 同时注册测试课的勿扰起止闹钟，让「上课自动开启 / 下课自动关闭」在测试里也跑得到。
     */
    suspend fun sendTestIslandNotification(context: Context) {
        if (!isSupported(context)) return

        val session = ReminderSession.CourseTest
        val courseName = TEST_COURSE_NAME
        val classroom = TEST_CLASSROOM
        val startMillis = System.currentTimeMillis() + TEST_COUNTDOWN_MS
        val endMillis = startMillis + TEST_CLASS_DURATION_MS
        val startTime = formatClock(startMillis)
        val endTime = formatClock(endMillis)

        // 清掉上一轮测试的「已上课 / 课中」岛，避免与本次倒计时岛并存
        cancelIslandState(context, session.islandStartedId)
        cancelIslandState(context, session.islandInClassId)

        // 测试岛写独立键位，不污染真实课前提醒 state
        LiveReminderState.saveIsland(
            notificationId = session.islandCountdownId,
            courseName = courseName,
            classroom = classroom,
            section = "",
            startTime = startTime,
            endTime = endTime,
            startMillis = startMillis,
            endMillis = endMillis,
            session = session,
        )
        // 测试课也要走「上课自动开启、下课自动关闭」，否则勿扰链路在测试里跑不到
        // 窗口直接由测试岛快照派生，见 LiveReminderState.testClassWindow
        CourseDayAlarmScheduler.syncTestClassDndAlarms(context)

        sendIslandNotification(
            context = context,
            notificationId = session.islandCountdownId,
            title = "$courseName $startTime",
            content = buildIslandContent(classroom),
            courseName = courseName,
            startTime = startTime,
            endTime = endTime,
            classroom = classroom,
            courseStartMillis = startMillis,
            session = session,
        )

        // 精确闹钟在倒计时结束时切「已上课」：只靠对账会滞后近一分钟
        scheduleIslandExpandAlarm(
            context = context,
            courseName = courseName,
            classroom = classroom,
            startTime = startTime,
            endTime = endTime,
            startMillis = startMillis,
            notificationId = session.islandCountdownId,
            session = session,
        )
        // 测试岛也要进刷新链，否则课中 ticker / 对账在进程被杀后无人驱动
        ReminderTickScheduler.scheduleNext(context)
    }

    // ————————————————————————— 排队与收起 —————————————————————————

    fun scheduleIslandExpandAlarm(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String,
        endTime: String?,
        startMillis: Long,
        notificationId: Int = ISLAND_NOTIFICATION_ID,
        session: ReminderSession = ReminderSession.Course,
    ) {
        val intent =
            Intent(context, IslandExpandReceiver::class.java).apply {
                putExtra(IslandExpandReceiver.EXTRA_COURSE_NAME, courseName)
                putExtra(IslandExpandReceiver.EXTRA_CLASSROOM, classroom)
                putExtra(IslandExpandReceiver.EXTRA_START_TIME, startTime)
                putExtra(IslandExpandReceiver.EXTRA_END_TIME, endTime ?: "")
                putExtra(IslandExpandReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(IslandExpandReceiver.EXTRA_COURSE_START_MILLIS, startMillis)
                putExtra(EXTRA_REMINDER_SESSION, session.name)
            }
        // RC 按自纪元起的分钟数 mod 100000 派生：同一天不会撞号；考试/测试课各开一段
        val rc =
            session.islandExpandRcBase + kotlin.math.abs((startMillis / 60_000L % 100_000L).toInt())
        ReminderAlarmScheduler.scheduleBoundary(
            context = context,
            triggerAtMs = startMillis,
            pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    rc,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
        )
    }

    private fun scheduleIslandDismiss(
        context: Context,
        notificationId: Int,
        startMillis: Long,
        session: ReminderSession = ReminderSession.Course,
    ) {
        scheduleIslandDismissAt(
            context = context,
            notificationId = notificationId,
            startMillis = startMillis,
            triggerAtMs = System.currentTimeMillis() + ISLAND_STARTED_VISIBLE_MS,
            session = session,
        )
    }

    private fun scheduleIslandDismissAt(
        context: Context,
        notificationId: Int,
        startMillis: Long,
        triggerAtMs: Long,
        session: ReminderSession = ReminderSession.Course,
    ) {
        val intent =
            Intent(context, IslandDismissReceiver::class.java).apply {
                putExtra(IslandDismissReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(IslandDismissReceiver.EXTRA_COURSE_START_MILLIS, startMillis)
                putExtra(EXTRA_REMINDER_SESSION, session.name)
            }
        val rc =
            session.islandDismissRcBase +
                kotlin.math.abs((startMillis / 60_000L % 100_000L).toInt())
        ReminderAlarmScheduler.scheduleBoundary(
            context = context,
            triggerAtMs = triggerAtMs,
            pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    rc,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
        )
    }

    fun cancelIslandState(context: Context, notificationId: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(notificationId)
        for (id in siblingIdsFor(notificationId)) manager?.cancel(id)
    }

    /** 只收课程侧三态（含测试课），考试侧由考试调度自己管。 */
    fun cancelAll(context: Context) {
        cancelSession(context, ReminderSession.Course)
        cancelSession(context, ReminderSession.CourseTest)
    }

    /** 收掉某会话的三态通知并清岛状态。 */
    fun cancelSession(context: Context, session: ReminderSession) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(session.islandCountdownId)
        manager?.cancel(session.islandStartedId)
        manager?.cancel(session.islandInClassId)
        LiveReminderState.clearIsland(session)
    }

    fun isInClassNotificationId(notificationId: Int): Boolean =
        ReminderSession.entries.any { it.islandInClassId == notificationId }

    fun sessionForIslandId(notificationId: Int): ReminderSession =
        ReminderSession.entries.firstOrNull {
            notificationId == it.islandCountdownId ||
                notificationId == it.islandStartedId ||
                notificationId == it.islandInClassId
        } ?: ReminderSession.Course

    fun isTestIslandId(notificationId: Int): Boolean =
        sessionForIslandId(notificationId).isTest

    fun activeIdFor(
        notificationId: Int,
        inClass: Boolean,
        session: ReminderSession = sessionForIslandId(notificationId),
    ): Int = if (inClass) session.islandInClassId else session.islandStartedId

    private fun countdownIdFor(notificationId: Int): Int =
        sessionForIslandId(notificationId).islandCountdownId

    private fun siblingIdsFor(notificationId: Int): IntArray {
        val session = sessionForIslandId(notificationId)
        return intArrayOf(
            session.islandCountdownId,
            session.islandStartedId,
            session.islandInClassId,
        ).filter { it != notificationId }.toIntArray()
    }

    // ————————————————————————— 对账 —————————————————————————

    /** 每分钟对账：兜底切换闹钟丢失/Doze 延迟、岛残留、开关关闭后的清理。课程/考试/测试三档都扫。 */
    fun reconcileIslandCountdown(context: Context) {
        val islandEnabled = isEnabled(context) && isSupported(context)

        if (!islandEnabled) {
            if (ReminderSession.entries.any { LiveReminderState.islandSnapshot(it) != null }) {
                val manager = context.getSystemService(NotificationManager::class.java)
                ReminderSession.entries.forEach { session ->
                    manager?.cancel(session.islandCountdownId)
                    manager?.cancel(session.islandStartedId)
                    manager?.cancel(session.islandInClassId)
                    LiveReminderState.clearIsland(session)
                }
            }
            return
        }

        ReminderSession.entries.forEach { session ->
            reconcileIslandState(context, session)
        }
    }

    private fun reconcileIslandState(context: Context, session: ReminderSession) {
        val state = LiveReminderState.islandSnapshot(session) ?: return
        val now = System.currentTimeMillis()

        val inClassEnabled = isInClassEnabled(context, session)
        val keepForInClass = inClassEnabled && state.endMillis > now
        val shouldShowInClass =
            keepForInClass &&
                shouldShowInClassNow(context, state.startMillis, state.endMillis, now, session)

        val expiredByEnd = state.endMillis > 0 && now >= state.endMillis
        val expiredByShow =
            !inClassEnabled && state.switched &&
                now - state.switchedAt >= ISLAND_STARTED_VISIBLE_MS
        if (expiredByEnd || expiredByShow) {
            cancelIslandState(context, state.notificationId)
            LiveReminderState.clearIsland(session)
            return
        }

        // 到点但还没切换（闹钟丢失/延迟）→ 立即补切，最多滞后一个刷新周期
        if (!state.switched && now >= state.startMillis) {
            if (now - state.startMillis > ISLAND_LATE_TOLERANCE_MS) {
                // 隔夜或重启后残留的状态：不要再补一个过期的「已上课」
                cancelIslandState(context, state.notificationId)
                LiveReminderState.clearIsland(session)
                return
            }
            onClassStart(
                context = context,
                courseName = state.courseName,
                classroom = state.classroom,
                startTime = state.startTime,
                endTime = state.endTime.ifEmpty { null },
                notificationId = state.notificationId,
                session = session,
            )
            return
        }

        if (inClassEnabled && now >= state.startMillis) {
            val alreadyInClass = isInClassNotificationId(state.notificationId)
            val canShow = state.endMillis > now
            if (canShow) {
                if (alreadyInClass) {
                    // 距下课数字由系统 timer 自刷，无需按分钟重推
                } else if (shouldShowInClass) {
                    sendInClassIslandNotification(
                        context = context,
                        courseName = state.courseName,
                        classroom = state.classroom,
                        startTime = state.startTime,
                        endTime = state.endTime,
                        notificationId = state.notificationId,
                        session = session,
                    )
                }
            }
            return
        }

        if (shouldShowInClass) {
            sendInClassIslandNotification(
                context = context,
                courseName = state.courseName,
                classroom = state.classroom,
                startTime = state.startTime,
                endTime = state.endTime,
                notificationId = state.notificationId,
                session = session,
            )
        }
    }

    private const val ISLAND_LATE_TOLERANCE_MS = 5 * 60_000L

    // ————————————————————————— JSON —————————————————————————

    private fun sendIslandNotification(
        context: Context,
        notificationId: Int,
        title: String,
        content: String,
        courseName: String? = null,
        startTime: String? = null,
        endTime: String? = null,
        classroom: String? = null,
        courseStartMillis: Long? = null,
        islandParamsOverride: String? = null,
        session: ReminderSession = ReminderSession.Course,
    ) {
        if (!isSupported(context)) return

        NotificationChannels.ensureLive(context)

        val contentIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            Notification.Builder(context, NotificationChannels.LIVE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        notification.extras.putString(
            "miui.focus.param",
            islandParamsOverride
                ?: buildIslandParamsJson(
                    context = context,
                    courseName = courseName,
                    startTime = startTime,
                    endTime = endTime,
                    classroom = classroom,
                    content = content,
                    courseStartMillis = courseStartMillis,
                    session = session,
                ),
        )
        notification.extras.putBundle("miui.focus.pics", buildPics(context))

        notifyIsland(context, notificationId, notification)
    }

    /**
     * 照 Nexio 的 withShizukuBypass：发岛前用 Shizuku 掐掉 XMSF 网络，发完立刻恢复。
     * 没装 Shizuku / 没授权就退化为普通 notify，行为与之前一致。
     */
    private fun notifyIsland(context: Context, notificationId: Int, notification: Notification) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!ShizukuManager.isAvailable()) {
            manager.notify(notificationId, notification)
            return
        }
        shizukuBypassLock.lock()
        try {
            val disabled =
                runCatching {
                    ShizukuManager.setXmsfNetworkingEnabled(context, false)
                }.getOrDefault(false)
            if (!disabled) {
                manager.notify(notificationId, notification)
                return
            }
            try {
                manager.notify(notificationId, notification)
                // 给系统一点时间把焦点通知送进岛
                Thread.sleep(100)
            } finally {
                runCatching { ShizukuManager.setXmsfNetworkingEnabled(context, true) }
            }
        } finally {
            shizukuBypassLock.unlock()
        }
    }

    private fun buildPics(context: Context): Bundle {
        val icon = Icon.createWithResource(context, R.mipmap.ic_launcher)
        val small = Icon.createWithResource(context, R.mipmap.ic_launcher_round)
        return Bundle().apply {
            putParcelable("miui.focus.pic_app_icon", icon)
            putParcelable("miui.focus.pic_app_icon_dark", icon)
            putParcelable("miui.focus.pic_small", small)
            putParcelable("miui.focus.pic_small_dark", small)
        }
    }

    /** 课前/考前倒计时 / 静态「已上课/已开考」通用的模板 2。 */
    private fun buildIslandParamsJson(
        context: Context,
        courseName: String?,
        startTime: String?,
        endTime: String?,
        classroom: String?,
        content: String,
        courseStartMillis: Long?,
        session: ReminderSession,
    ): String {
        val labels = session.labels
        val p = prefs()
        val labelRule = ClassroomLabel.read(p)
        val leftMode = p.getInt(SettingsKeys.ISLAND_LEFT_MODE, 0)
        val rightMode = p.getInt(SettingsKeys.ISLAND_RIGHT_MODE, 1)
        val aodMode = p.getInt(SettingsKeys.ISLAND_AOD_MODE, 0)

        val now = System.currentTimeMillis()
        // 槽位、息屏标题、展开态「地点」行只有十几个半角宽，超了系统从尾部截断（丢掉的正好是房号），
        // 所以缩略态一律喂精简后的文本；展开态放得下完整地点。
        val shortClassroom = ClassroomLabel.apply(classroom.orEmpty(), labelRule)
        val shortCourse = ClassroomLabel.applyCourse(courseName.orEmpty(), labelRule)
        val remainMs = courseStartMillis?.let { it - now }
        val counting = remainMs != null && remainMs > 0
        val minutesUntil = if (counting) ((remainMs + 59_999L) / 60_000L).toInt() else 0

        val islandLeftText =
            when (leftMode) {
                0 -> shortCourse
                1 -> shortClassroom
                2 -> if (counting) minutesShort(minutesUntil) else labels.started
                else -> courseName.orEmpty()
            }
        val islandRightText =
            when (rightMode) {
                0 -> shortCourse
                1 -> shortClassroom
                2 -> if (counting) minutesShort(minutesUntil) else labels.started
                else -> shortClassroom
            }
        val aodTitle =
            if (aodMode == 1) {
                shortClassroom.ifEmpty { shortCourse.ifEmpty { courseName.orEmpty() } }
            } else {
                shortCourse.ifEmpty { courseName.orEmpty() }
            }

        val json = JSONObject()
        val paramV2 =
            JSONObject().apply {
                put("business", labels.business)
                put("protocol", 1)
                if (counting) {
                    put("enableFloat", true)
                } else {
                    // 已上课/已开考：不弹悬浮窗，只走岛
                    put("islandFirstFloat", true)
                    put("enableFloat", false)
                }
                put("updatable", true)
                put("outEffectSrc", OUT_EFFECT)
                put("reopen", "reopen")
                put("sequence", sequenceCounter.incrementAndGet())
                put("aodTitle", aodTitle)

                put(
                    "baseInfo",
                    JSONObject().apply {
                        put("type", 2)
                        put("title", courseName.orEmpty())
                        put("content", content)
                        put("subTitle", "")
                        put("extraTitle", "")
                        put("specialTitle", "")
                        put("subContent", "")
                        put("picFunction", "")
                        put("showDivider", true)
                        put("showContentDivider", false)
                        put("colorTitle", "#111111")
                        put("colorTitleDark", "#ffffff")
                        put("colorContent", "#333333")
                        put("colorContentDark", "#cccccc")
                    },
                )

                put(
                    "picInfo",
                    JSONObject().apply {
                        put("type", 1)
                        put("pic", "")
                    },
                )

                put(
                    "hintInfo",
                    JSONObject().apply {
                        put("type", 2)
                        if (counting) {
                            put("content", labels.upcoming)
                            put("title", "")
                        } else {
                            put("content", NOW)
                            put("title", labels.started)
                        }
                        put(
                            "timerInfo",
                            JSONObject().apply {
                                if (counting) {
                                    put("timerType", -1)
                                    put("timerWhen", courseStartMillis)
                                    put("timerTotal", 0L)
                                    put("timerSystemCurrent", now)
                                } else {
                                    put("timerType", 0)
                                    put("timerWhen", 0)
                                    put("timerTotal", 0L)
                                    put("timerSystemCurrent", 0)
                                }
                            },
                        )
                        put("subContent", labels.placeLabel)
                        put("subTitle", classroom.orEmpty())
                        put("colorContent", "#666666")
                        put("colorContentDark", "#aaaaaa")
                        put("colorTitle", "#222222")
                        put("colorTitleDark", "#eeeeee")
                        put("colorSubContent", "#666666")
                        put("colorSubContentDark", "#aaaaaa")
                        put("colorSubTitle", "#222222")
                        put("colorSubTitleDark", "#eeeeee")
                        put(
                            "actionInfo",
                            JSONObject().apply {
                                put("actionTitle", labels.dndAction)
                                put("actionIntentType", 2)
                                put("actionIntent", dndToggleIntent(context, session))
                            },
                        )
                    },
                )

                put(
                    "param_island",
                    JSONObject().apply {
                        put("islandProperty", 1)
                        put("islandTimeout", 3600)
                        put(
                            "bigIslandArea",
                            JSONObject().apply {
                                put("templateNo", 2)
                                put(
                                    "imageTextInfoLeft",
                                    JSONObject().apply {
                                        put("type", 1)
                                        put(
                                            "textInfo",
                                            JSONObject().apply {
                                                put("title", islandLeftText)
                                                put("content", "")
                                                put("showHighlightColor", false)
                                                put("narrowFont", false)
                                            },
                                        )
                                    },
                                )
                                if (rightMode == 2 && counting) {
                                    put(
                                        "sameWidthDigitInfo",
                                        JSONObject().apply {
                                            put("content", labels.startsIn)
                                            put("showHighlightColor", false)
                                            put(
                                                "timerInfo",
                                                JSONObject().apply {
                                                    put("timerType", -1)
                                                    put("timerWhen", courseStartMillis)
                                                    put("timerTotal", 0L)
                                                    put("timerSystemCurrent", now)
                                                },
                                            )
                                        },
                                    )
                                    put(
                                        "textInfo",
                                        JSONObject().apply {
                                            put("frontTitle", "")
                                            put("title", "")
                                            put("content", "")
                                            put("showHighlightColor", false)
                                            put("narrowFont", false)
                                        },
                                    )
                                } else {
                                    put(
                                        "textInfo",
                                        JSONObject().apply {
                                            put("frontTitle", "")
                                            put("title", if (counting) islandRightText else labels.started)
                                            put("content", "")
                                            put("showHighlightColor", false)
                                            put("narrowFont", false)
                                        },
                                    )
                                }
                            },
                        )
                        put(
                            "smallIslandArea",
                            JSONObject().apply {
                                put(
                                    "picInfo",
                                    JSONObject().apply {
                                        put("type", 1)
                                        put("pic", "miui.focus.pic_small")
                                        put("picDark", "miui.focus.pic_small_dark")
                                    },
                                )
                            },
                        )
                    },
                )
            }
        json.put("param_v2", paramV2)
        return json.toString()
    }

    /** 课中/考中：模板 2，A 区名称，B 区「正在上课/正在考试」或距结束倒计时。 */
    private fun buildInClassIslandParamsJson(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String,
        endTime: String,
        courseEndMillis: Long,
        session: ReminderSession,
    ): String {
        val labels = session.labels
        val p = prefs()
        val labelRule = ClassroomLabel.read(p)
        val aodMode = p.getInt(SettingsKeys.ISLAND_AOD_MODE, 0)
        val inClassRightMode = p.getInt(SettingsKeys.ISLAND_IN_CLASS_RIGHT_MODE, 0)

        val now = System.currentTimeMillis()
        val counting = courseEndMillis > now
        val shortClassroom = ClassroomLabel.apply(classroom, labelRule)
        val aodTitle =
            if (aodMode == 1) {
                shortClassroom.ifEmpty { courseName }
            } else {
                courseName
            }
        val baseContent =
            buildString {
                if (startTime.isNotEmpty()) append(startTime)
                if (endTime.isNotEmpty() && endTime != startTime) {
                    if (isNotEmpty()) append(" - ")
                    append(endTime)
                }
            }

        val json = JSONObject()
        val paramV2 =
            JSONObject().apply {
                put("business", labels.business)
                put("protocol", 1)
                put("islandFirstFloat", true)
                put("enableFloat", false)
                put("updatable", true)
                put("outEffectSrc", OUT_EFFECT)
                put("reopen", "reopen")
                put("sequence", sequenceCounter.incrementAndGet())
                put("aodTitle", aodTitle)

                put(
                    "baseInfo",
                    JSONObject().apply {
                        put("type", 2)
                        put("title", courseName)
                        put("content", baseContent)
                        put("subTitle", "")
                        put("extraTitle", "")
                        put("specialTitle", "")
                        put("subContent", "")
                        put("picFunction", "")
                        put("showDivider", true)
                        put("showContentDivider", false)
                        put("colorTitle", "#111111")
                        put("colorTitleDark", "#ffffff")
                        put("colorContent", "#333333")
                        put("colorContentDark", "#cccccc")
                    },
                )

                put(
                    "picInfo",
                    JSONObject().apply {
                        put("type", 1)
                        put("pic", "")
                    },
                )

                put(
                    "hintInfo",
                    JSONObject().apply {
                        put("type", 2)
                        put("content", if (counting) labels.untilSessionEnds else labels.sessionEnded)
                        put("title", "")
                        put(
                            "timerInfo",
                            JSONObject().apply {
                                if (counting) {
                                    put("timerType", -1)
                                    put("timerWhen", courseEndMillis)
                                    put("timerTotal", 0L)
                                    put("timerSystemCurrent", now)
                                } else {
                                    put("timerType", 0)
                                    put("timerWhen", 0)
                                    put("timerTotal", 0L)
                                    put("timerSystemCurrent", 0)
                                }
                            },
                        )
                        put("subContent", labels.placeLabel)
                        put("subTitle", shortClassroom)
                        put("colorContent", "#666666")
                        put("colorContentDark", "#aaaaaa")
                        put("colorTitle", "#222222")
                        put("colorTitleDark", "#eeeeee")
                        put("colorSubContent", "#666666")
                        put("colorSubContentDark", "#aaaaaa")
                        put("colorSubTitle", "#222222")
                        put("colorSubTitleDark", "#eeeeee")
                        put(
                            "actionInfo",
                            JSONObject().apply {
                                put("actionTitle", labels.viewAction)
                                put("actionIntentType", 1)
                                put("actionIntent", OPEN_APP_INTENT_TEMPLATE.format(context.packageName))
                            },
                        )
                    },
                )

                put(
                    "param_island",
                    JSONObject().apply {
                        put("islandProperty", 1)
                        put("islandTimeout", 3600)
                        put(
                            "bigIslandArea",
                            JSONObject().apply {
                                put("templateNo", 2)
                                put(
                                    "imageTextInfoLeft",
                                    JSONObject().apply {
                                        put("type", 1)
                                        put(
                                            "textInfo",
                                            JSONObject().apply {
                                                put("title", courseName)
                                                put("content", "")
                                                put("showHighlightColor", false)
                                                put("narrowFont", false)
                                            },
                                        )
                                    },
                                )
                                if (inClassRightMode == 1 && counting) {
                                    put(
                                        "sameWidthDigitInfo",
                                        JSONObject().apply {
                                            put("content", labels.endsIn)
                                            put("showHighlightColor", false)
                                            put(
                                                "timerInfo",
                                                JSONObject().apply {
                                                    put("timerType", -1)
                                                    put("timerWhen", courseEndMillis)
                                                    put("timerTotal", 0L)
                                                    put("timerSystemCurrent", now)
                                                },
                                            )
                                        },
                                    )
                                    put(
                                        "textInfo",
                                        JSONObject().apply {
                                            put("frontTitle", "")
                                            put("title", "")
                                            put("content", "")
                                            put("showHighlightColor", false)
                                            put("narrowFont", false)
                                        },
                                    )
                                } else {
                                    put(
                                        "textInfo",
                                        JSONObject().apply {
                                            put("frontTitle", "")
                                            put("title", if (counting) labels.inSessionNow else labels.sessionEnded)
                                            put("content", "")
                                            put("showHighlightColor", false)
                                            put("narrowFont", false)
                                        },
                                    )
                                }
                            },
                        )
                        put(
                            "smallIslandArea",
                            JSONObject().apply {
                                put(
                                    "picInfo",
                                    JSONObject().apply {
                                        put("type", 1)
                                        put("pic", "miui.focus.pic_small")
                                        put("picDark", "miui.focus.pic_small_dark")
                                    },
                                )
                            },
                        )
                    },
                )
            }
        json.put("param_v2", paramV2)
        return json.toString()
    }

    private fun buildIslandContent(classroom: String): String = classroom

    private fun dndToggleIntent(context: Context, session: ReminderSession): String {
        val receiver =
            if (session.isExam) ExamDndReceiver::class.java.name else ClassDndReceiver::class.java.name
        val action =
            if (session.isExam) ExamDndReceiver.ACTION_TOGGLE else ClassDndReceiver.ACTION_TOGGLE
        return "intent:#Intent;action=$action;component=${context.packageName}/$receiver;end"
    }

    private fun formatClock(millis: Long): String {
        if (millis <= 0L) return ""
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(millis))
    }

    private fun minutesShort(minutes: Int): String = "${minutes}分钟"

    private const val OUT_EFFECT = "outer_glow"
    private const val NOW = "现在"
    private const val OPEN_APP_INTENT_TEMPLATE =
        "intent:#Intent;component=%s/restarhalf.stellar.schedule.MainActivity;launchFlags=0x14000000;end"
}