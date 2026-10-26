package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

interface TermSettingsPort {

    fun observeSelectedTerm(): Flow<String>

    fun setSelectedTerm(term: String)

    fun observeActiveScheduleTerm(): Flow<String>

    fun setActiveScheduleTerm(term: String)

    fun observeCurrentTermId(): Flow<String>

    fun setCurrentTermId(termId: String)

    fun observeCachedSemesterIds(): Flow<List<String>>

    fun setCachedSemesterIds(semesterIds: List<String>)

    suspend fun shouldAutoSyncAndMark(nowMs: Long): Boolean
}
