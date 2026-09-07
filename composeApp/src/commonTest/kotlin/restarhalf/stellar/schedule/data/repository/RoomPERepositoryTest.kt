package restarhalf.stellar.schedule.data.repository

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.local.dao.PEDetailDao
import restarhalf.stellar.schedule.data.local.dao.PEYearScoreDao
import restarhalf.stellar.schedule.data.local.entity.PEDetailSummaryEntity
import restarhalf.stellar.schedule.data.local.entity.PESubjectScoreEntity
import restarhalf.stellar.schedule.data.local.entity.PEYearScoreEntity
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PESubjectScore
import restarhalf.stellar.schedule.data.remote.PEYearScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomPERepositoryTest {

    private val yearScores = MutableStateFlow(
        listOf(PEYearScoreEntity(schoolYear = "2025-2026", total = 88.5, isFree = 0, done = 4, nums = 6)),
    )
    private val summary = MutableStateFlow<PEDetailSummaryEntity?>(null)
    private val detailScores = MutableStateFlow<List<PESubjectScoreEntity>>(emptyList())

    private val yearDao = mock<PEYearScoreDao>(MockMode.autofill) {
        every { observeAllScores() } returns yearScores
    }
    private val detailDao = mock<PEDetailDao>(MockMode.autofill) {
        every { observeDetailSummary(any()) } returns summary
        every { observeDetailScores(any()) } returns detailScores
    }

    private val repo = RoomPERepository(yearDao, detailDao)

    @Test
    fun observeAllScoresMapsToDomain() = runTest {
        val scores = repo.observeAllScores().first()
        assertEquals(1, scores.size)
        assertTrue(scores.single() is PEYearScore)
        assertEquals(88.5, scores.single().total)
    }

    @Test
    fun observeDetailDataNullWhenNoSummary() = runTest {
        assertNull(repo.observeDetailData("2025-2026").first())
    }

    @Test
    fun observeDetailDataCombinesSummaryAndScores() = runTest {
        summary.value = PEDetailSummaryEntity(schoolYear = "2025-2026", totalScore = 76.0, totalGrade = "良好")
        detailScores.value = listOf(
            PESubjectScoreEntity(
                schoolYear = "2025-2026", subjectId = "s1", subName = "跑步",
                result = "3'40\"", score = 90, unit = "min", subRatio = "20%", grade = "优", isJoin = 1,
            ),
        )
        val detail = repo.observeDetailData("2025-2026").first()
        assertEquals(76.0, detail?.totalScore)
        assertEquals("良好", detail?.totalGrade)
        assertEquals(listOf("s1"), detail?.dataArr?.map { it.subjectId })
    }

    @Test
    fun replaceScoresDelegates() = runTest {
        val captured = mutableListOf<List<PEYearScoreEntity>>()
        everySuspend { yearDao.replaceAll(any()) } calls { (list: List<PEYearScoreEntity>) ->
            captured.add(list)
            Unit
        }
        repo.replaceScores(listOf(PEYearScore(schoolYear = "y", total = 1.0, isFree = 0, done = 1, nums = 1)))
        assertEquals(listOf("y"), captured.single().map { it.schoolYear })
    }

    @Test
    fun saveDetailDataSplitsSummaryAndScores() = runTest {
        val capturedYear = mutableListOf<String>()
        val capturedSummary = mutableListOf<PEDetailSummaryEntity?>()
        val capturedScores = mutableListOf<List<PESubjectScoreEntity>>()
        everySuspend { detailDao.replaceDetailByYear(any(), any(), any()) } calls {
            (year: String, sum: PEDetailSummaryEntity?, scores: List<PESubjectScoreEntity>) ->
            capturedYear.add(year)
            capturedSummary.add(sum)
            capturedScores.add(scores)
            Unit
        }
        val detail = PEDetailData(
            totalScore = 76.0,
            totalGrade = "良好",
            dataArr = listOf(PESubjectScore(subjectId = "s1", subName = "跑步", score = 90, isJoin = 1)),
        )
        repo.saveDetailData("2025-2026", detail)
        assertEquals("2025-2026", capturedYear.single())
        assertEquals(76.0, capturedSummary.single()?.totalScore)
        assertEquals(listOf("s1"), capturedScores.single().map { it.subjectId })
    }

    @Test
    fun clearAllDeletesPeData() = runTest {
        everySuspend { detailDao.deleteAllPeData() } returns Unit
        repo.clearAll()
        verifySuspend { detailDao.deleteAllPeData() }
    }
}
