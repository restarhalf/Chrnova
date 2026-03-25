package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

interface SettingsPort {
    fun observeShowNonCurrentWeek(): Flow<Boolean>
    fun setShowNonCurrentWeek(show: Boolean)

    fun observeThemeMode(): Flow<Int>
    fun setThemeMode(mode: Int)

    fun observeSelectedTerm(): Flow<String>
    fun setSelectedTerm(term: String)

    fun observeCourseReminderEnabled(): Flow<Boolean>
    fun setCourseReminderEnabled(enabled: Boolean)

    fun observeExamReminderEnabled(): Flow<Boolean>
    fun setExamReminderEnabled(enabled: Boolean)

    fun observeFloatingBar(): Flow<Int>
    fun setFloatingBar(mode: Int)

    suspend fun shouldAutoSyncAndMark(nowMs: Long): Boolean
}
