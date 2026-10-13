package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow


interface SettingsPort {
    companion object {
        
        const val DEFAULT_ROW_HEIGHT_DP = 64
        
        const val MIN_ROW_HEIGHT_DP = 48
        
        const val MAX_ROW_HEIGHT_DP = 120
    }

    
    fun observeShowNonCurrentWeek(): Flow<Boolean>
    
    fun setShowNonCurrentWeek(show: Boolean)

    
    fun observeScheduleRowHeight(): Flow<Int>
    
    fun setScheduleRowHeight(heightDp: Int)

    
    fun observeThemeMode(): Flow<Int>
    
    fun setThemeMode(mode: Int)

    
    fun observeSelectedTerm(): Flow<String>
    
    fun setSelectedTerm(term: String)

    
    fun observeActiveScheduleTerm(): Flow<String>
    
    fun setActiveScheduleTerm(term: String)

    
    fun observeCourseReminderEnabled(): Flow<Boolean>
    
    fun setCourseReminderEnabled(enabled: Boolean)

    
    fun observeExamReminderEnabled(): Flow<Boolean>
    
    fun setExamReminderEnabled(enabled: Boolean)

    
    fun observeFloatingBar(): Flow<Int>
    
    fun setFloatingBar(mode: Int)

    
    fun observeLogEnabled(): Flow<Boolean>
    
    fun setLogEnabled(enabled: Boolean)

    
    suspend fun shouldAutoSyncAndMark(nowMs: Long): Boolean

    
    fun getDeviceId(): String

    
    fun getStarVerified(): Boolean

    
    fun setStarVerified(verified: Boolean)

    
    fun observeCachedSemesterIds(): Flow<List<String>>

    
    fun setCachedSemesterIds(semesterIds: List<String>)

    
    fun observeCurrentTermId(): Flow<String>

    
    fun setCurrentTermId(termId: String)

    
    fun getUserAvatarUri(): String?

    
    fun setUserAvatarUri(uri: String?)

    
    fun getUserNickname(): String?

    
    fun setUserNickname(nickname: String?)
}
