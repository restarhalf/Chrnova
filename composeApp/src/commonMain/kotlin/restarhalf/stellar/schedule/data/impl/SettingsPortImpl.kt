package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.SettingsPort
import kotlin.random.Random


@OptIn(ExperimentalSettingsApi::class)
class SettingsPortImpl(
    private val settings: ObservableSettings,
) : SettingsPort {

    
    override fun observeShowNonCurrentWeek(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.SHOW_NON_CURRENT_WEEK, true)
    }

    
    override fun setShowNonCurrentWeek(show: Boolean) {
        settings[SettingsKeys.SHOW_NON_CURRENT_WEEK] = show
    }

    
    override fun observeScheduleRowHeight(): Flow<Int> {
        return settings.getIntFlow(
            SettingsKeys.SCHEDULE_ROW_HEIGHT,
            SettingsPort.DEFAULT_ROW_HEIGHT_DP,
        )
    }

    
    override fun setScheduleRowHeight(heightDp: Int) {
        settings[SettingsKeys.SCHEDULE_ROW_HEIGHT] = heightDp.coerceIn(
            SettingsPort.MIN_ROW_HEIGHT_DP,
            SettingsPort.MAX_ROW_HEIGHT_DP,
        )
    }

    
    override fun observeThemeMode(): Flow<Int> {
        return settings.getIntFlow(SettingsKeys.THEME_MODE, 0)
    }

    
    override fun setThemeMode(mode: Int) {
        settings[SettingsKeys.THEME_MODE] = mode.coerceIn(0, 2)
    }

    
    override fun observeFloatingBar(): Flow<Int> {
        return settings.getIntFlow(SettingsKeys.FLOATING_BAR, 0)
    }

    
    override fun setFloatingBar(mode: Int) {
        settings[SettingsKeys.FLOATING_BAR] = mode.coerceIn(0,2)
    }

    
    override fun observeSelectedTerm(): Flow<String> {
        return settings.getStringFlow(SettingsKeys.SELECTED_TERM, "")
    }

    
    override fun setSelectedTerm(term: String) {
        settings[SettingsKeys.SELECTED_TERM] = term
    }

    
    override fun observeActiveScheduleTerm(): Flow<String> {
        return settings.getStringFlow(SettingsKeys.ACTIVE_SCHEDULE_TERM, "")
    }

    
    override fun setActiveScheduleTerm(term: String) {
        settings[SettingsKeys.ACTIVE_SCHEDULE_TERM] = 
            if (term!="") 
                term
            else
                settings.getString(
                    SettingsKeys.CURRENT_TERM_ID,
                    defaultValue = ""
                )
    }

    
    override fun observeCourseReminderEnabled(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.REMINDER_ENABLED, false)
    }

    
    override fun setCourseReminderEnabled(enabled: Boolean) {
        settings[SettingsKeys.REMINDER_ENABLED] = enabled
    }

    
    override fun observeExamReminderEnabled(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.EXAM_REMINDER_ENABLED, false)
    }

    
    override fun setExamReminderEnabled(enabled: Boolean) {
        settings[SettingsKeys.EXAM_REMINDER_ENABLED] = enabled
    }

    override fun observeLogEnabled(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.LOG_ENABLED, false)
    }

    override fun setLogEnabled(enabled: Boolean) {
        settings[SettingsKeys.LOG_ENABLED] = enabled
    }

    
    override suspend fun shouldAutoSyncAndMark(nowMs: Long): Boolean {
        val lastAutoSyncMs = settings.getLong(SettingsKeys.LAST_AUTO_SYNC_MS, 0L)
        val cooldownMs = 24L * 60L * 60L * 1000L
        return if (nowMs - lastAutoSyncMs >= cooldownMs) {
            settings[SettingsKeys.LAST_AUTO_SYNC_MS] = nowMs
            true
        } else {
            false
        }
    }

    override fun getDeviceId(): String {
        val existing = settings.getStringOrNull(SettingsKeys.DEVICE_ID)
        if (!existing.isNullOrEmpty()) return existing
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val id = buildString(32) {
            repeat(32) { append(chars[Random.nextInt(chars.length)]) }
        }
        settings[SettingsKeys.DEVICE_ID] = id
        return id
    }

    override fun getStarVerified(): Boolean {
        return settings.getBoolean(SettingsKeys.STAR_VERIFIED, false)
    }

    override fun setStarVerified(verified: Boolean) {
        settings[SettingsKeys.STAR_VERIFIED] = verified
    }

    override fun observeCachedSemesterIds(): Flow<List<String>> {
        return settings.getStringFlow(SettingsKeys.CACHED_SEMESTER_IDS, "[]")
            .map { json ->
                try {
                    Json.decodeFromString<List<String>>(json)
                } catch (e: Exception) {
                    emptyList()
                }
            }
    }

    override fun setCachedSemesterIds(semesterIds: List<String>) {
        settings[SettingsKeys.CACHED_SEMESTER_IDS] = Json.encodeToString(semesterIds)
    }

    override fun observeCurrentTermId(): Flow<String> {
        return settings.getStringFlow(SettingsKeys.CURRENT_TERM_ID, "")
    }

    override fun setCurrentTermId(termId: String) {
        settings[SettingsKeys.CURRENT_TERM_ID] = termId
    }

    override fun getUserAvatarUri(): String? {
        val uri = settings.getStringOrNull(SettingsKeys.USER_AVATAR_URI)
        return if (uri.isNullOrBlank()) null else uri
    }

    override fun setUserAvatarUri(uri: String?) {
        if (uri != null) {
            settings[SettingsKeys.USER_AVATAR_URI] = uri
        } else {
            settings[SettingsKeys.USER_AVATAR_URI] = ""
        }
    }

    override fun getUserNickname(): String? {
        val nickname = settings.getStringOrNull(SettingsKeys.USER_NICKNAME)
        return if (nickname.isNullOrBlank()) null else nickname
    }

    override fun setUserNickname(nickname: String?) {
        if (nickname != null) {
            settings[SettingsKeys.USER_NICKNAME] = nickname
        } else {
            settings[SettingsKeys.USER_NICKNAME] = ""
        }
    }
}
