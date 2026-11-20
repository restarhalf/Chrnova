package restarhalf.stellar.schedule.reminder

import android.content.Context

/**
 * 课前/考前提醒发出后的实况承载选择，照 Nexio 的 sendPreClassNotification：
 *
 * - 岛可用且开启 → 超级岛倒计时 + 到点展开闹钟；
 * - 否则 → 原生实况倒计时（含到点切换闹钟）。
 *
 * 触发点已过但刚开始（连堂课间为 0 / 补发迟到）时直接落「已上课/已开考」，
 * 超过宽限期就不再打扰。课程与考试共用，仅 [ReminderSession] 不同。
 */
internal object PreClassLiveDispatcher {

    // 开始后仍允许补发「已开始」的宽限期，超出则不再打扰
    private const val START_GRACE_MS = 2 * 60_000L

    fun dispatch(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String,
        endTime: String,
        startMillis: Long,
        endMillis: Long,
        session: ReminderSession = ReminderSession.Course,
    ) {
        val now = System.currentTimeMillis()
        val safeEnd =
            if (endMillis > startMillis) endMillis else startMillis + session.defaultDurationMs
        val islandMode = IslandNotificationHelper.isEnabled(context) &&
            IslandNotificationHelper.isSupported(context)

        if (startMillis <= now) {
            if (now - startMillis > START_GRACE_MS) return
            if (islandMode) {
                IslandNotificationHelper.sendPreClassIslandNotification(
                    context = context,
                    courseName = courseName,
                    classroom = classroom,
                    startTime = startTime,
                    startMillis = startMillis,
                    endMillis = safeEnd,
                    session = session,
                )
            } else {
                // 先落状态再按当前时间对账：能进进行中就切进行中，否则闪「已开始」
                LiveReminderState.saveCountdown(
                    courseName = courseName,
                    classroom = classroom,
                    section = "",
                    startTime = startTime,
                    startMillis = startMillis,
                    endMillis = safeEnd,
                    session = session,
                )
                LiveNotificationController.updateActiveCountdown(context)
            }
            return
        }

        if (islandMode) {
            IslandNotificationHelper.sendPreClassIslandNotification(
                context = context,
                courseName = courseName,
                classroom = classroom,
                startTime = startTime,
                startMillis = startMillis,
                endMillis = safeEnd,
                session = session,
            )
            IslandNotificationHelper.scheduleIslandExpandAlarm(
                context = context,
                courseName = courseName,
                classroom = classroom,
                startTime = startTime,
                endTime = endTime,
                startMillis = startMillis,
                session = session,
            )
        } else {
            LiveNotificationController.showPreClassCountdownNotification(
                context = context,
                courseName = courseName,
                classroom = classroom,
                startTime = startTime,
                startMillis = startMillis,
                endMillis = safeEnd,
                session = session,
            )
        }
    }
}