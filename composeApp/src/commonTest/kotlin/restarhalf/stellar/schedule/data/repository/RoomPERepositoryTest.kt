package restarhalf.stellar.schedule.data.repository

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
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
import restarhalf.stellar.schedule.domain.model.PEDetailData
import restarhalf.stellar.schedule.domain.model.PESubjectScore
import restarhalf.stellar.schedule.domain.model.PEYearScore
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomPERepositoryTest {

    private val userFlow = MutableStateFlow("U1")

    private val yearScores = MutableStateFlow(
        listOf(
            PEYearScoreEntity(
                userNo = "U1",
                schoolYear = "2025-2026",
                total = 88.5,
                isFree = 0,
                done = 4,
                nums = 6,
            ),
        ),
    )
    private val summary = MutableStateFlow<PEDetailSummaryEntity?>(null)
    private val detailScores = MutableStateFlow<List<PESubjectScoreEntity>>(emptyList())

    private val yearDao = mock<PEYearScoreDao>(MockMode.autofill) {
        every { observeAllScores(any()) } returns yearScores
    }
    private val detailDao = mock<PEDetailDao>(MockMode.autofill) {
        every { observeDetailSummary(any(), any()) } returns summary
        every { observeDetailScores(any(), any()) } returns detailScores
    }
    private val auth = mock<PEAuthPort>(MockMode.autofill) {
        every { observeUserId() } returns userFlow
    }

    private val repo = RoomPERepositoryImpl(yearDao, detailDao, auth)

    @Test
    fun observeAllScoresMapsToDomain() = runTest {
        val scores = repo.observeAllScores().first()
        assertEquals(1, scores.size)
        assertTrue(scores.single() is PEYearScore)
        assertEquals(88.5, scores.single().total)
    }

    @Test
    fun observeAllScoresBlankUserReturnsEmpty() = runTest {
        userFlow.value = ""
        assertTrue(repo.observeAllScores().first().isEmpty())
    }

    @Test
    fun observeDetailDataNullWhenNoSummary() = runTest {
        assertNull(repo.observeDetailData("2025-2026").first())
    }

    @Test
    fun observeDetailDataCombinesSummaryAndScores() = runTest {
        summary.value = PEDetailSummaryEntity(
            userNo = "U1",
            schoolYear = "2025-2026",
            totalScore = 76.0,
            totalGrade = "良好",
        )
        detailScores.value = listOf(
            PESubjectScoreEntity(
                userNo = "U1",
                schoolYear = "2025-2026",
                subjectId = "s1",
                subName = "跑步",
                result = "3'40\"",
                score = 90,
                unit = "min",
                subRatio = "20%",
                grade = "优",
                isJoin = 1,
            ),
        )
        val detail = repo.observeDetailData("2025-2026").first()
        assertEquals(76.0, detail?.totalScore)
        assertEquals("良好", detail?.totalGrade)
        assertEquals(listOf("s1"), detail?.dataArr?.map { it.subjectId })
    }

    @Test
    fun replaceScoresStampsCurrentUser() = runTest {
        val capturedUsers = mutableListOf<String>()
        val captured = mutableListOf<List<PEYearScoreEntity>>()
        everySuspend { yearDao.replaceAll(any(), any()) } calls {
            (userNo: String, list: List<PEYearScoreEntity>) ->
            capturedUsers.add(userNo)
            captured.add(list)
            Unit
        }
        repo.replaceScores(
            listOf(PEYearScore(schoolYear = "y", total = 1.0, isFree = 0, done = 1, nums = 1)),
        )
        assertEquals("U1", capturedUsers.single())
        assertEquals(listOf("y"), captured.single().map { it.schoolYear })
        assertTrue(captured.single().all { it.userNo == "U1" })
    }

    @Test
    fun replaceScoresSkipsWhenNoUser() = runTest {
        userFlow.value = ""
        repo.replaceScores(emptyList())
        verifySuspend(dev.mokkery.verify.VerifyMode.not) { yearDao.replaceAll(any(), any()) }
    }

    @Test
    fun saveDetailDataSplitsSummaryAndScores() = runTest {
        val capturedUsers = mutableListOf<String>()
        val capturedYear = mutableListOf<String>()
        val capturedSummary = mutableListOf<PEDetailSummaryEntity?>()
        val capturedScores = mutableListOf<List<PESubjectScoreEntity>>()
        everySuspend { detailDao.replaceDetailByYear(any(), any(), any(), any()) } calls {
            (
                userNo: String,
                year: String,
                sum: PEDetailSummaryEntity?,
                scores: List<PESubjectScoreEntity>,
            ) ->
            capturedUsers.add(userNo)
            capturedYear.add(year)
            capturedSummary.add(sum)
            capturedScores.add(scores)
            Unit
        }
        val detail = PEDetailData(
            totalScore = 76.0,
            totalGrade = "良好",
            dataArr = listOf(
                PESubjectScore(subjectId = "s1", subName = "跑步", score = 90, isJoin = 1),
            ),
        )
        repo.saveDetailData("2025-2026", detail)
        assertEquals("U1", capturedUsers.single())
        assertEquals("2025-2026", capturedYear.single())
        assertEquals(76.0, capturedSummary.single()?.totalScore)
        assertEquals(listOf("s1"), capturedScores.single().map { it.subjectId })
        assertTrue(capturedScores.single().all { it.userNo == "U1" })
    }

    @Test
    fun clearUserDeletesOnlyThatUser() = runTest {
        repo.clearUser("U9")
        verifySuspend { detailDao.deletePeDataByUserNo("U9") }
    }

    @Test
    fun clearUserIgnoresBlankUser() = runTest {
        repo.clearUser("")
        verifySuspend(dev.mokkery.verify.VerifyMode.not) { detailDao.deletePeDataByUserNo(any()) }
    }
}
