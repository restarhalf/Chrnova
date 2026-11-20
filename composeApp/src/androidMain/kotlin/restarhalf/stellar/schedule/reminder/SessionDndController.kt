package restarhalf.stellar.schedule.reminder

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.widget.Toast
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import kotlinx.coroutines.CancellationException

/**
 * 上课/考试自动勿扰，状态机照 Nexio 的 ClassDndHelper，课程与考试各一套
 * （[ReminderSession.Course] / [ReminderSession.Exam]），开关与快照键独立：
 *
 * - 不靠「开始/结束」两条闹钟直接改状态，而是 [applyCurrentState] 按当前时间对账，
 *   闹钟只是对账的触发点；闹钟丢了、Doze 延迟了，下一次对账仍能把状态修正过来。
 * - 接管前把铃声、勿扰两条通道的原始值都拍快照，结束还原快照而不是粗暴恢复成响铃/ALL。
 * - 结束还原带一致性校验：系统里仍是本应用写入的值才还原，用户进行中手动改过就尊重用户。
 * - 还原失败（权限被回收）保留会话等待重试，超过 12 小时才放弃。
 * - 两套会话各自对账、各自交还：一套接管中时另一套不重复接管，
 *   由仍在进行中的那套在结束时还原快照。
 */
internal object SessionDndController : KoinComponent {

    private const val MODE_SILENT = 1
    private const val MODE_PRIORITY = 2

    /** 还原失败后最多保留会话的时长 */
    private const val DEFER_ABANDON_MS = 12 * 60 * 60 * 1000L

    /** 非课堂/考场时段手动开启的勿扰最多保持的时长（跨日同样失效） */
    private const val MANUAL_HOLD_MAX_MS = 8 * 60 * 60 * 1000L

    private fun prefs(): ObservableSettings = get(named(SettingsKeys.PREFS_NAME))

    // ————————————————————————— 按时间对账 —————————————————————————

    /** 闹钟 / 重排 / 开关切换 / 开机后共同调用，幂等。 */
    suspend fun applyCurrentState(
        context: Context,
        session: ReminderSession,
        inSession: Boolean,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val prefs = prefs()
        val keys = session.settingsKeys
        val featureEnabled =
            prefs.getBoolean(keys.dndEnabled, false) &&
                prefs.getBoolean(SettingsKeys.REMINDER_ENABLED, false)
        if (!featureEnabled) {
            handBack(context, session, requireConsistency = false)
            return
        }
        if (!inSession) {
            // 用户从通知/超级岛按钮手动开启的：不在时段内也保持，等用户再点一次才还原
            if (isManualHeld(context, session, prefs)) return
            handBack(context, session, requireConsistency = true)
            return
        }
        if (!isPolicyGranted(context)) {
            // 进入时段但没授权：既不能接管也不能还原，保持现状
            handBack(context, session, requireConsistency = false)
            return
        }

        val mode = prefs.getInt(keys.dndMode, 0)
        // 手动接管一旦进入时段就转交时段生命周期，结束才能自动还原
        if (prefs.getBoolean(keys.dndManual, false)) {
            prefs.remove(keys.dndManual)
            prefs.remove(keys.dndManualAt)
        }
        if (!prefs.getBoolean(keys.dndApplied, false)) {
            // 另一套会话已接管（课连着考等重叠场景）：不重复接管、不覆盖它的快照
            if (otherSessionApplied(prefs, session)) return
            takeOver(context, session, prefs, mode)
            return
        }
        val appliedMode = prefs.getInt(keys.dndAppliedMode, 0)
        if (appliedMode != mode) {
            // 换档位：先还原旧档位写的那条通道，快照沿用不重拍
            restorePrimary(context, session, prefs, appliedMode)
            if (writeState(context, mode)) recordAppliedState(context, session, prefs, mode)
        }
        // 已接管且仍在时段内：只观察，不强行覆盖用户进行中的手动改动
    }

    /** 开关关闭 / 提醒总开关关闭：用户明确要求停止管理，无条件还原。 */
    fun handBackFromUser(context: Context, session: ReminderSession) {
        handBack(context, session, requireConsistency = false)
    }

    /**
     * 通知 / 超级岛「上课勿扰 / 考试勿扰」按钮：点一下立刻生效，再点一下立刻还原。
     * 非时段内开启记为用户手动接管，[applyCurrentState] 不会自动把它还原掉。
     */
    suspend fun toggleFromNotification(
        context: Context,
        session: ReminderSession,
        inSession: Boolean,
    ) {
        val prefs = prefs()
        val keys = session.settingsKeys
        val labels = session.labels
        if (!prefs.getBoolean(SettingsKeys.REMINDER_ENABLED, false) &&
            !prefs.getBoolean(SettingsKeys.NEXT_DAY_REMINDER_ENABLED, false)
        ) {
            toast(context, "请先在「课程提醒」中开启课程提醒")
            return
        }

        val next = !prefs.getBoolean(keys.dndEnabled, false)
        prefs[keys.dndEnabled] = next

        if (!next) {
            handBack(context, session, requireConsistency = false)
            toast(context, "已关闭${labels.dndAction}")
            return
        }

        if (!isPolicyGranted(context)) {
            // 回滚开关：否则它停在开启态却永远不生效
            prefs[keys.dndEnabled] = false
            toast(context, "请先在「课程提醒」中授予勿扰权限")
            return
        }
        val mode = prefs.getInt(keys.dndMode, 0)

        // 已接管时先交还，避免上一次的快照被覆盖
        if (prefs.getBoolean(keys.dndApplied, false)) {
            handBack(context, session, requireConsistency = false)
        }
        // 另一套会话正接管：只开开关，等它结束后本会话对账时再接管
        if (!otherSessionApplied(prefs, session) && !takeOver(context, session, prefs, mode)) {
            prefs[keys.dndEnabled] = false
            toast(context, "开启失败，请检查勿扰权限")
            return
        }
        // 时段内交给时段生命周期（结束自动恢复）；时段外保持到用户再点一次
        markManual(prefs, keys, held = !inSession)

        val what =
            when (mode) {
                MODE_SILENT -> "静音"
                MODE_PRIORITY -> "勿扰模式"
                else -> "完全勿扰"
            }
        val endHint = if (session.isExam) "考试结束后" else "下课后"
        toast(context, "已开启$what${if (inSession) "，${endHint}自动关闭" else "，再点一次关闭"}")
    }

    private fun toast(context: Context, text: String) {
        try {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // 后台广播里 Toast 偶发不可用，忽略
        }
    }

    /** 手动接管中：不在时段内也不自动还原；跨日或超时视为遗留状态，顺带还原并清除标记。 */
    private fun isManualHeld(
        context: Context,
        session: ReminderSession,
        prefs: ObservableSettings,
    ): Boolean {
        val keys = session.settingsKeys
        if (!prefs.getBoolean(keys.dndManual, false)) return false
        val at = prefs.getLong(keys.dndManualAt, 0L)
        val now = System.currentTimeMillis()
        val expired =
            at <= 0L ||
                now - at > MANUAL_HOLD_MAX_MS ||
                !isSameDay(at, now)
        if (!expired) return true
        handBack(context, session, requireConsistency = false)
        return false
    }

    private fun markManual(prefs: ObservableSettings, keys: SessionSettingsKeys, held: Boolean) {
        prefs[keys.dndManual] = held
        prefs[keys.dndManualAt] = System.currentTimeMillis()
    }

    private fun otherSessionApplied(prefs: ObservableSettings, session: ReminderSession): Boolean {
        val other = if (session.isExam) ReminderSession.Course else ReminderSession.Exam
        return prefs.getBoolean(other.settingsKeys.dndApplied, false)
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val ca = java.util.Calendar.getInstance().apply { timeInMillis = a }
        val cb = java.util.Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) &&
            ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
    }

    /**
     * 重启 / 应用升级后系统状态已被重置，而接管标记还留在存储里。
     * 不清掉的话按时间对账会以为「已接管」而只观察，本时段就再也不会进入勿扰。
     */
    fun dropStaleSessionAfterReboot(context: Context) {
        val prefs = prefs()
        listOf(ReminderSession.Course, ReminderSession.Exam).forEach { session ->
            if (prefs.getBoolean(session.settingsKeys.dndApplied, false)) {
                clearSession(prefs, session.settingsKeys)
            }
        }
    }

    // ————————————————————————— 接管与交还 —————————————————————————

    private fun takeOver(
        context: Context,
        session: ReminderSession,
        prefs: ObservableSettings,
        mode: Int,
    ): Boolean {
        if (!isPolicyGranted(context)) return false
        val before = readCurrentState(context, mode)
        snapshotOriginal(context, session, prefs)
        if (!writeState(context, mode)) return false

        val accepted = readCurrentState(context, mode) ?: targetState(mode)
        // 写入前后没变化且不等于目标值 → 系统拒绝写入（权限回收 / ROM 拦截）
        if (before != null && accepted == before && accepted != targetState(mode)) return false

        recordAppliedState(context, session, prefs, mode)
        return true
    }

    private fun handBack(context: Context, session: ReminderSession, requireConsistency: Boolean) {
        val prefs = prefs()
        val keys = session.settingsKeys
        if (!prefs.getBoolean(keys.dndApplied, false)) {
            clearSessionIfAny(prefs, keys)
            return
        }
        val mode = prefs.getInt(keys.dndAppliedMode, 0)

        var restoreRinger = true
        var restoreFilter = true
        if (requireConsistency) {
            val current = readCurrentState(context, mode)
            if (current == null) {
                clearSession(prefs, keys)
                return
            }
            val expected = appliedValue(prefs, keys, mode)
            if (current != expected && current != targetState(mode)) {
                // 用户进行中自己改过，保持现状
                clearSession(prefs, keys)
                return
            }
            if (secondaryChannelTouched(context, prefs, keys, mode)) {
                if (mode == MODE_SILENT) restoreFilter = false else restoreRinger = false
            }
        }

        // 先勿扰后铃声：关勿扰时系统会联动恢复铃声，顺序反了会被覆盖
        val filterOk = !restoreFilter || restoreFilter(context, session, prefs)
        val ringerOk = !restoreRinger || restoreRinger(context, session, prefs)
        if (!(filterOk && ringerOk)) {
            // 写不回去多半是权限被回收：保留快照等权限恢复，超过 12 小时才放弃
            val now = System.currentTimeMillis()
            val first = prefs.getLong(keys.dndDeferAt, 0L)
            if (first <= 0L) {
                prefs[keys.dndDeferAt] = now
            } else if (now - first > DEFER_ABANDON_MS) {
                clearSession(prefs, keys)
            }
            return
        }
        clearSession(prefs, keys)
    }

    // ————————————————————————— 读写两条系统通道 —————————————————————————

    private fun targetState(mode: Int): Int = when (mode) {
        MODE_SILENT -> AudioManager.RINGER_MODE_SILENT
        MODE_PRIORITY -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        else -> NotificationManager.INTERRUPTION_FILTER_NONE
    }

    private fun readCurrentState(context: Context, mode: Int): Int? =
        if (mode == MODE_SILENT) readRinger(context) else readFilter(context)

    private fun readRinger(context: Context): Int? =
        try {
            context.getSystemService(AudioManager::class.java)?.ringerMode
        } catch (e: Exception) {
            null
        }

    private fun readFilter(context: Context): Int? =
        try {
            context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
        } catch (e: Exception) {
            null
        }

    /** 接管后实测到的值；没观测到就退回目标值 */
    private fun appliedValue(prefs: ObservableSettings, keys: SessionSettingsKeys, mode: Int): Int {
        val observed =
            if (mode == MODE_SILENT) {
                prefs.takeIf { it.hasKey(keys.dndSetRinger) }?.getInt(keys.dndSetRinger, 0)
            } else {
                prefs.takeIf { it.hasKey(keys.dndSetFilter) }?.getInt(keys.dndSetFilter, 0)
            }
        return observed ?: targetState(mode)
    }

    private fun secondaryChannelTouched(
        context: Context,
        prefs: ObservableSettings,
        keys: SessionSettingsKeys,
        mode: Int,
    ): Boolean {
        val set =
            if (mode == MODE_SILENT) {
                prefs.takeIf { it.hasKey(keys.dndSetFilter) }?.getInt(keys.dndSetFilter, 0)
            } else {
                prefs.takeIf { it.hasKey(keys.dndSetRinger) }?.getInt(keys.dndSetRinger, 0)
            }
        val current = if (mode == MODE_SILENT) readFilter(context) else readRinger(context)
        return set != null && current != null && current != set
    }

    private fun writeState(context: Context, mode: Int): Boolean =
        try {
            when (mode) {
                MODE_SILENT ->
                    context.getSystemService(AudioManager::class.java)
                        ?.ringerMode = AudioManager.RINGER_MODE_SILENT
                MODE_PRIORITY ->
                    context.getSystemService(NotificationManager::class.java)
                        ?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                else ->
                    context.getSystemService(NotificationManager::class.java)
                        ?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
            true
        } catch (e: Exception) {
            false
        }

    // ————————————————————————— 快照与还原 —————————————————————————

    private fun snapshotOriginal(
        context: Context,
        session: ReminderSession,
        prefs: ObservableSettings,
    ) {
        val keys = session.settingsKeys
        readRinger(context)?.let { prefs[keys.dndOriginalRinger] = it }
            ?: prefs.remove(keys.dndOriginalRinger)
        readFilter(context)?.let { prefs[keys.dndOriginalFilter] = it }
            ?: prefs.remove(keys.dndOriginalFilter)
    }

    private fun recordAppliedState(
        context: Context,
        session: ReminderSession,
        prefs: ObservableSettings,
        mode: Int,
    ) {
        val keys = session.settingsKeys
        prefs[keys.dndApplied] = true
        prefs[keys.dndAppliedMode] = mode
        readRinger(context)?.let { prefs[keys.dndSetRinger] = it }
            ?: prefs.remove(keys.dndSetRinger)
        readFilter(context)?.let { prefs[keys.dndSetFilter] = it }
            ?: prefs.remove(keys.dndSetFilter)
    }

    private fun restorePrimary(
        context: Context,
        session: ReminderSession,
        prefs: ObservableSettings,
        mode: Int,
    ) {
        if (mode == MODE_SILENT) restoreRinger(context, session, prefs)
        else restoreFilter(context, session, prefs)
    }

    private fun restoreRinger(
        context: Context,
        session: ReminderSession,
        prefs: ObservableSettings,
    ): Boolean {
        val keys = session.settingsKeys
        if (!prefs.hasKey(keys.dndOriginalRinger)) return true
        if (!isPolicyGranted(context)) return false
        val original = prefs.getInt(keys.dndOriginalRinger, AudioManager.RINGER_MODE_NORMAL)
        return try {
            context.getSystemService(AudioManager::class.java)?.ringerMode = original
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun restoreFilter(
        context: Context,
        session: ReminderSession,
        prefs: ObservableSettings,
    ): Boolean {
        val keys = session.settingsKeys
        if (!prefs.hasKey(keys.dndOriginalFilter)) return true
        if (!isPolicyGranted(context)) return false
        val original =
            prefs.getInt(keys.dndOriginalFilter, NotificationManager.INTERRUPTION_FILTER_ALL)
        return try {
            context.getSystemService(NotificationManager::class.java)
                ?.setInterruptionFilter(original)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun clearSession(prefs: ObservableSettings, keys: SessionSettingsKeys) {
        prefs[keys.dndApplied] = false
        prefs.remove(keys.dndAppliedMode)
        prefs.remove(keys.dndOriginalRinger)
        prefs.remove(keys.dndOriginalFilter)
        prefs.remove(keys.dndSetRinger)
        prefs.remove(keys.dndSetFilter)
        prefs.remove(keys.dndDeferAt)
        prefs.remove(keys.dndManual)
        prefs.remove(keys.dndManualAt)
    }

    private fun clearSessionIfAny(prefs: ObservableSettings, keys: SessionSettingsKeys) {
        if (prefs.hasKey(keys.dndAppliedMode) ||
            prefs.hasKey(keys.dndOriginalRinger) ||
            prefs.hasKey(keys.dndOriginalFilter) ||
            prefs.hasKey(keys.dndDeferAt) ||
            prefs.hasKey(keys.dndManual)
        ) {
            clearSession(prefs, keys)
        }
    }

    private fun isPolicyGranted(context: Context): Boolean =
        try {
            context.getSystemService(NotificationManager::class.java)
                ?.isNotificationPolicyAccessGranted == true
        } catch (e: Exception) {
            false
        }
}