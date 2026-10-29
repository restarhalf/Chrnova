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
import restarhalf.stellar.schedule.data.local.dao.ExaminationDao
import restarhalf.stellar.schedule.data.local.entity.ExaminationEntity
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RoomExaminationRepositoryTest {

    private fun entity(marker: String, semesterId: String = "S1", userNo: String = "U1") = ExaminationEntity(
        id = 3L,
        courseNumber = "MA101",
        courseName = marker,
        semesterId = semesterId,
        userNo = userNo,
    )

    private val profileFlow = MutableStateFlow(JwxtAuthProfile())

    private val dao = mock<ExaminationDao>(MockMode.autofill) {
        every { observeExaminationsByUserNo(any()) } returns MutableStateFlow(listOf(entity("byUser")))
        every { observeExaminationById(any()) } returns MutableStateFlow<ExaminationEntity?>(null)
    }

    private val auth = mock<JwxtAuthPort> {
        every { observeProfile() } returns profileFlow
    }

    private val repo = RoomExaminationRepository(dao, auth)

    @Test
    fun observeByUserNoDelegates() = runTest {
        assertEquals(listOf("byUser"), repo.observeExaminationsByUserNo("U1").first().map { it.courseName })
    }

    @Test
    fun observeByBlankUserNoReturnsEmpty() = runTest {
        assertTrue(repo.observeExaminationsByUserNo("").first().isEmpty())
    }

    @Test
    fun getAllOnceUsesCurrentUser() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        everySuspend { dao.getAllExaminationsByUserNoOnce("U1") } returns listOf(entity("once"))
        assertEquals(listOf("once"), repo.getAllExaminationsOnce().map { it.courseName })
    }

    @Test
    fun getAllOnceBlankUserReturnsEmpty() = runTest {
        assertTrue(repo.getAllExaminationsOnce().isEmpty())
    }

    @Test
    fun observeByIdOnlyReturnsCurrentUsersExam() = runTest {
        assertEquals(null, repo.observeExaminationById(1L).first())
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        every { dao.observeExaminationById(9L) } returns MutableStateFlow(entity("single"))
        assertEquals("single", repo.observeExaminationById(9L).first()?.courseName)

        every { dao.observeExaminationById(10L) } returns MutableStateFlow(entity("other", userNo = "U2"))
        assertEquals(null, repo.observeExaminationById(10L).first())
    }

    @Test
    fun saveExaminationOverridesSemesterId() = runTest {
        val captured = mutableListOf<ExaminationEntity>()
        everySuspend { dao.insertExamination(any()) } calls { (e: ExaminationEntity) ->
            captured.add(e)
            7L
        }
        val exam = entity("exam").toDomain().copy(semesterId = "OLD")
        assertEquals(7L, repo.saveExamination(exam, "NEW-SEM"))
        assertEquals("NEW-SEM", captured.single().semesterId)
    }

    @Test
    fun deleteDelegates() = runTest {
        everySuspend { dao.deleteExamination(5L) } returns Unit
        repo.deleteExamination(5L)
        verifySuspend { dao.deleteExamination(5L) }
    }

    @Test
    fun replaceExaminationsScopesUserAndSemester() = runTest {
        val capturedUsers = mutableListOf<String>()
        val capturedSem = mutableListOf<String>()
        val capturedList = mutableListOf<List<ExaminationEntity>>()
        everySuspend { dao.replaceBySemester(any(), any(), any()) } calls {
            (userNo: String, sem: String, list: List<ExaminationEntity>) ->
            capturedUsers.add(userNo)
            capturedSem.add(sem)
            capturedList.add(list)
            Unit
        }
        val exams = listOf(
            entity("a", userNo = "U2").toDomain(),
            entity("b").toDomain(),
        )
        repo.replaceExaminations("U1", "SEM-X", exams)
        assertEquals("U1", capturedUsers.single())
        assertEquals("SEM-X", capturedSem.single())
        assertEquals(listOf("a", "b"), capturedList.single().map { it.courseName })
        assertTrue(capturedList.single().all { it.userNo == "U1" && it.semesterId == "SEM-X" })
    }

    @Test
    fun bindUnboundDelegates() = runTest {
        everySuspend { dao.bindUnboundExaminations("U2") } returns Unit
        repo.bindUnboundExaminations("U2")
        verifySuspend { dao.bindUnboundExaminations("U2") }
    }

    @Test
    fun replaceExaminationsRejectsBlankUser() = runTest {
        assertFailsWith<IllegalArgumentException> {
            repo.replaceExaminations("", "SEM-X", emptyList())
        }
    }

    @Test
    fun fixInvalidSemesterIdsRewritesEmptyAndManual() = runTest {
        everySuspend { dao.updateSemesterId(any(), any()) } returns Unit
        repo.fixInvalidSemesterIds("FIXED")
        verifySuspend { dao.updateSemesterId("", "FIXED") }
        verifySuspend { dao.updateSemesterId("manual", "FIXED") }
    }
}
