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
import restarhalf.stellar.schedule.data.local.dao.GradeDao
import restarhalf.stellar.schedule.data.local.entity.GradeEntity
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.domain.model.GradeCourse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomGradeRepositoryTest {

    private fun entity(marker: String, userNo: String = "U1") = GradeEntity(
        courseCode = "CS102",
        courseName = marker,
        score = "92",
        gradePoint = 4.0,
        credit = 3.0,
        userNo = userNo,
        semester = "S1",
    )

    private val dao = mock<GradeDao>(MockMode.autofill) {
        every { observeAllGrades() } returns MutableStateFlow(listOf(entity("all")))
        every { observeGradesByUserNo(any()) } returns MutableStateFlow(listOf(entity("byUser")))
    }

    private val repo = RoomGradeRepository(dao)

    @Test
    fun observeAllMapsToDomain() = runTest {
        val grades = repo.observeAllGrades().first()
        assertEquals(listOf("all"), grades.map { it.courseName })
        assertTrue(grades.single() is GradeCourse)
    }

    @Test
    fun observeByUserNoDelegates() = runTest {
        assertEquals(
            listOf("byUser"),
            repo.observeGradesByUserNo("U1").first().map { it.courseName },
        )
    }

    @Test
    fun getAllGradesByUserNoMaps() = runTest {
        everySuspend { dao.getAllGradesByUserNo("U1") } returns listOf(entity("once"))
        assertEquals(listOf("once"), repo.getAllGradesByUserNo("U1").map { it.courseName })
    }

    @Test
    fun replaceGradesDelegatesToSemester() = runTest {
        val captured = mutableListOf<Pair<String, List<GradeEntity>>>()
        everySuspend { dao.replaceBySemester(any(), any()) } calls { (sem: String, list: List<GradeEntity>) ->
            captured.add(sem to list)
            Unit
        }
        val grades = listOf(entity("g1").toDomain())
        repo.replaceGrades("S1", grades)
        assertEquals("S1", captured.single().first)
        assertEquals(listOf("g1"), captured.single().second.map { it.courseName })
    }

    @Test
    fun replaceGradesByUserNoAndSemesterStampsUserNo() = runTest {
        val capturedUserNos = mutableListOf<String>()
        val capturedSemesters = mutableListOf<String>()
        everySuspend { dao.replaceByUserNoAndSemester(any(), any(), any()) } calls {
            (userNo: String, sem: String, list: List<GradeEntity>) ->
            capturedUserNos.add(userNo)
            capturedSemesters.add(sem)
            Unit
        }
        val grades = listOf(entity("g1", userNo = "OLD").toDomain())
        repo.replaceGradesByUserNoAndSemester("NEW", "S1", grades)
        assertEquals("NEW", capturedUserNos.single())
        assertEquals("S1", capturedSemesters.single())
    }

    @Test
    fun clearAllDelegates() = runTest {
        everySuspend { dao.deleteAll() } returns Unit
        repo.clearAll()
        verifySuspend { dao.deleteAll() }
    }
}
