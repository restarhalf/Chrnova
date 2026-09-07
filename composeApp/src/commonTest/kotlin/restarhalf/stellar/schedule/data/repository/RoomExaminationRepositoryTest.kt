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
import restarhalf.stellar.schedule.domain.model.Examination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomExaminationRepositoryTest {

    private fun entity(marker: String, semesterId: String = "S1") = ExaminationEntity(
        id = 3L,
        courseNumber = "MA101",
        courseName = marker,
        semesterId = semesterId,
    )

    private val dao = mock<ExaminationDao>(MockMode.autofill) {
        every { observeAllExaminations() } returns MutableStateFlow(listOf(entity("all")))
        every { observeExaminationsByUserNo(any()) } returns MutableStateFlow(listOf(entity("byUser")))
        every { observeExaminationById(any()) } returns MutableStateFlow<ExaminationEntity?>(null)
    }

    private val repo = RoomExaminationRepository(dao)

    @Test
    fun observeAllMapsToDomain() = runTest {
        val exams = repo.observeAllExaminations().first()
        assertEquals(1, exams.size)
        assertEquals("all", exams.single().courseName)
        assertTrue(exams.single() is Examination)
    }

    @Test
    fun observeByUserNoDelegates() = runTest {
        assertEquals(listOf("byUser"), repo.observeExaminationsByUserNo("U1").first().map { it.courseName })
    }

    @Test
    fun observeByIdMapsNullAndValue() = runTest {
        assertEquals(null, repo.observeExaminationById(1L).first())
        every { dao.observeExaminationById(9L) } returns MutableStateFlow(entity("single"))
        assertEquals("single", repo.observeExaminationById(9L).first()?.courseName)
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
    fun deleteAndClearDelegate() = runTest {
        everySuspend { dao.deleteExamination(5L) } returns Unit
        everySuspend { dao.deleteAll() } returns Unit
        repo.deleteExamination(5L)
        repo.clearAll()
        verifySuspend { dao.deleteExamination(5L) }
        verifySuspend { dao.deleteAll() }
    }

    @Test
    fun replaceExaminationsOverridesSemesterId() = runTest {
        val capturedSem = mutableListOf<String>()
        val capturedList = mutableListOf<List<ExaminationEntity>>()
        everySuspend { dao.replaceBySemester(any(), any()) } calls { (sem: String, list: List<ExaminationEntity>) ->
            capturedSem.add(sem)
            capturedList.add(list)
            Unit
        }
        val exams = listOf(entity("a").toDomain(), entity("b").toDomain())
        repo.replaceExaminations("SEM-X", exams)
        assertEquals("SEM-X", capturedSem.single())
        assertEquals(listOf("a", "b"), capturedList.single().map { it.courseName })
        assertTrue(capturedList.single().all { it.semesterId == "SEM-X" })
    }

    @Test
    fun bindUnboundDelegates() = runTest {
        everySuspend { dao.bindUnboundExaminations("U2") } returns Unit
        repo.bindUnboundExaminations("U2")
        verifySuspend { dao.bindUnboundExaminations("U2") }
    }

    @Test
    fun fixInvalidSemesterIdsRewritesEmptyAndManual() = runTest {
        everySuspend { dao.updateSemesterId(any(), any()) } returns Unit
        repo.fixInvalidSemesterIds("FIXED")
        verifySuspend { dao.updateSemesterId("", "FIXED") }
        verifySuspend { dao.updateSemesterId("manual", "FIXED") }
    }
}
