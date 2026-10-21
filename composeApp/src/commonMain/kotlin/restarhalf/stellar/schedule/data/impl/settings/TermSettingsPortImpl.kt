package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.TermSettingsPort


@OptIn(ExperimentalSettingsApi::class)
class TermSettingsPortImpl(
    private val settings: ObservableSettings,
) : TermSettingsPort {


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
            if (term != "")
                term
            else
                settings.getString(
                    SettingsKeys.CURRENT_TERM_ID,
                    defaultValue = ""
                )
    }


    override fun observeCurrentTermId(): Flow<String> {
        return settings.getStringFlow(SettingsKeys.CURRENT_TERM_ID, "")
    }


    override fun setCurrentTermId(termId: String) {
        settings[SettingsKeys.CURRENT_TERM_ID] = termId
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
}
