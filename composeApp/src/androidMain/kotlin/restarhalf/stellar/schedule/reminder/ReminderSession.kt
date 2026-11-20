package restarhalf.stellar.schedule.reminder

import android.content.Intent
import restarhalf.stellar.schedule.domain.model.SettingsKeys

/** 广播里携带会话种类的 extra key。 */
internal const val EXTRA_REMINDER_SESSION = "reminder_session"

internal fun sessionFromIntent(intent: Intent): ReminderSession =
    runCatching {
        ReminderSession.valueOf(intent.getStringExtra(EXTRA_REMINDER_SESSION).orEmpty())
    }.getOrDefault(ReminderSession.Course)

/** 一次会话用到的设置键。课程与考试各一套，测试课沿用课程那套。 */
internal data class SessionSettingsKeys(
    val inSessionEnabled: String,
    val inSessionTimingMode: String,
    val inSessionLeadMinutes: String,
    val dndEnabled: String,
    val dndMode: String,
    val dndApplied: String,
    val dndAppliedMode: String,
    val dndOriginalRinger: String,
    val dndOriginalFilter: String,
    val dndSetRinger: String,
    val dndSetFilter: String,
    val dndDeferAt: String,
    val dndManual: String,
    val dndManualAt: String,
)

private val CourseSettingsKeys = SessionSettingsKeys(
    inSessionEnabled = SettingsKeys.IN_CLASS_REMINDER_ENABLED,
    inSessionTimingMode = SettingsKeys.IN_CLASS_TIMING_MODE,
    inSessionLeadMinutes = SettingsKeys.IN_CLASS_LEAD_MINUTES,
    dndEnabled = SettingsKeys.CLASS_DND_ENABLED,
    dndMode = SettingsKeys.CLASS_DND_MODE,
    dndApplied = SettingsKeys.CLASS_DND_APPLIED,
    dndAppliedMode = SettingsKeys.CLASS_DND_APPLIED_MODE,
    dndOriginalRinger = SettingsKeys.CLASS_DND_ORIGINAL_RINGER,
    dndOriginalFilter = SettingsKeys.CLASS_DND_ORIGINAL_FILTER,
    dndSetRinger = SettingsKeys.CLASS_DND_SET_RINGER,
    dndSetFilter = SettingsKeys.CLASS_DND_SET_FILTER,
    dndDeferAt = SettingsKeys.CLASS_DND_DEFER_AT,
    dndManual = SettingsKeys.CLASS_DND_MANUAL,
    dndManualAt = SettingsKeys.CLASS_DND_MANUAL_AT,
)

private val ExamSettingsKeys = SessionSettingsKeys(
    inSessionEnabled = SettingsKeys.IN_EXAM_REMINDER_ENABLED,
    inSessionTimingMode = SettingsKeys.IN_EXAM_TIMING_MODE,
    inSessionLeadMinutes = SettingsKeys.IN_EXAM_LEAD_MINUTES,
    dndEnabled = SettingsKeys.EXAM_DND_ENABLED,
    dndMode = SettingsKeys.EXAM_DND_MODE,
    dndApplied = SettingsKeys.EXAM_DND_APPLIED,
    dndAppliedMode = SettingsKeys.EXAM_DND_APPLIED_MODE,
    dndOriginalRinger = SettingsKeys.EXAM_DND_ORIGINAL_RINGER,
    dndOriginalFilter = SettingsKeys.EXAM_DND_ORIGINAL_FILTER,
    dndSetRinger = SettingsKeys.EXAM_DND_SET_RINGER,
    dndSetFilter = SettingsKeys.EXAM_DND_SET_FILTER,
    dndDeferAt = SettingsKeys.EXAM_DND_DEFER_AT,
    dndManual = SettingsKeys.EXAM_DND_MANUAL,
    dndManualAt = SettingsKeys.EXAM_DND_MANUAL_AT,
)

internal val ReminderSession.settingsKeys: SessionSettingsKeys
    get() = if (isExam) ExamSettingsKeys else CourseSettingsKeys

/** 结束时间缺失时的兜底时长：单节课 45 分钟，一场考试 2 小时。 */
internal val ReminderSession.defaultDurationMs: Long
    get() = if (isExam) 120 * 60_000L else 45 * 60_000L

/**
 * 一次提醒会话的种类。课程与考试共用同一套「超级岛/实况三态 + 分钟对账 + 勿扰」链路，
 * 仅通知 ID、状态键前缀、文案、勿扰开关与 business 不同；课程测试课再单列一档，互不覆盖。
 */
internal enum class ReminderSession(
    val keyPrefix: String,
    val labels: ReminderLabels,
    /** 实况三态 ID */
    val liveCountdownId: Int,
    val liveStartedId: Int,
    val liveInClassId: Int,
    /** 超级岛三态 ID */
    val islandCountdownId: Int,
    val islandStartedId: Int,
    val islandInClassId: Int,
    /** 岛展开/收起闹钟 request code 段 */
    val islandExpandRcBase: Int,
    val islandDismissRcBase: Int,
) {
    Course(
        keyPrefix = "",
        labels = CourseLabels,
        liveCountdownId = 2003,
        liveStartedId = 2004,
        liveInClassId = 2005,
        islandCountdownId = 1003,
        islandStartedId = 1004,
        islandInClassId = 1005,
        islandExpandRcBase = 70000,
        islandDismissRcBase = 72000,
    ),
    CourseTest(
        keyPrefix = "test_",
        labels = CourseLabels,
        liveCountdownId = 5100,
        liveStartedId = 5101,
        liveInClassId = 5102,
        islandCountdownId = 5000,
        islandStartedId = 5001,
        islandInClassId = 5002,
        islandExpandRcBase = 76000,
        islandDismissRcBase = 78000,
    ),
    Exam(
        keyPrefix = "exam_",
        labels = ExamLabels,
        liveCountdownId = 2103,
        liveStartedId = 2104,
        liveInClassId = 2105,
        islandCountdownId = 1103,
        islandStartedId = 1104,
        islandInClassId = 1105,
        islandExpandRcBase = 74000,
        islandDismissRcBase = 75000,
    ),
    ;

    val isTest: Boolean get() = this == CourseTest
    val isExam: Boolean get() = this == Exam

    companion object {
        /** 真实会话（对账链每次都要扫的两档）。 */
        val real = listOf(Course, Exam)
    }
}