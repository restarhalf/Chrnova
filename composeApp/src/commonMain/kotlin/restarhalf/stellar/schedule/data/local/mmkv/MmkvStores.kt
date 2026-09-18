package restarhalf.stellar.schedule.data.local.mmkv

import restarhalf.stellar.schedule.domain.model.SettingsKeys

/**
 * 应用内全部 named Settings 存储名，与 multiplatform-settings / SP / NSUserDefaults 对齐。
 */
object MmkvStores {
    const val MIGRATION_FLAG = "__chrnova_mmkv_migrated_v1"

    val ALL: List<String> = listOf(
        SettingsKeys.PREFS_NAME,
        "reminder_codes",
        "jwxt_auth",
        "pe_auth",
        "timetable_prefs",
    )
}
