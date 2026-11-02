package restarhalf.stellar.schedule.data.repository

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.local.dao.CourseDao
import restarhalf.stellar.schedule.data.local.entity.CourseEntity
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RoomCourseRepositoryTest {

    private val termFlow = MutableStateFlow("")
    private val profileFlow = MutableStateFlow(JwxtAuthProfile())

    private fun entity(marker: String, userNo: String = "U1") = CourseEntity(
        id = 1L,
        name = marker,
        location = "L",
        teacher = "T",
        dayOfWeek = 1,
        startSection = 1,
        sectionCount = 1,
        weeks = listOf(1),
        color = "#000000",
        userNo = userNo,
    )

    private val dao = mock<CourseDao>(MockMode.autofill) {
        every { getCoursesByUserNo(any()) } returns MutableStateFlow(listOf(entity("byUser")))
        every { getCoursesByUserNoAndSemester(any(), any()) } returns
            MutableStateFlow(listOf(entity("byUserAndSemester")))
        every { getCourseById(any()) } returns MutableStateFlow<CourseEntity?>(null)
    }

    private val settings = mock<TermSettingsPort> {
        every { observeActiveScheduleTerm() } returns termFlow
    }
    private val auth = mock<JwxtAuthPort> {
        every { observeProfile() } returns profileFlow
    }

    private val repo = RoomCourseRepository(dao, settings, auth)

    @Test
    fun observeAllBlankUserReturnsEmpty() = runTest {
        val names = repo.observeAllCourses().first().map { it.name }
        assertTrue(names.isEmpty())
    }

    @Test
    fun observeAllUserOnlyUsesUserNo() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        val names = repo.observeAllCourses().drop(1).first().map { it.name }
        assertEquals(listOf("byUser"), names)
    }

    @Test
    fun observeAllSemesterWithoutUserReturnsEmpty() = runTest {
        termFlow.value = "S1"
        val names = repo.observeAllCourses().first().map { it.name }
        assertTrue(names.isEmpty())
    }

    @Test
    fun observeAllUserAndSemesterUsesCombined() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        termFlow.value = "S1"
        val names = repo.observeAllCourses().drop(1).first().map { it.name }
        assertEquals(listOf("byUserAndSemester"), names)
    }

    @Test
    fun observeAllMapsEntityToDomain() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        val course = repo.observeAllCourses().drop(1).first().single()
        assertEquals(1L, course.id)
        assertTrue(course is Course)
    }

    @Test
    fun observeCourseByIdFilteredByUserAndActiveSemester() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        val fixed = CourseEntity(
            id = 5L, name = "定课", semesterId = "S1", userNo = "U1",
            location = "L", teacher = "T", dayOfWeek = 1, startSection = 1,
            sectionCount = 1, weeks = listOf(1), color = "#000000",
        )
        every { dao.getCourseById(5L) } returns MutableStateFlow(fixed)

        assertEquals("定课", repo.observeCourseById(5L).first()?.name)

        termFlow.value = "S1"
        assertEquals("定课", repo.observeCourseById(5L).first()?.name)

        termFlow.value = "S2"
        assertEquals(null, repo.observeCourseById(5L).first())
    }

    @Test
    fun observeCourseByIdRejectsBlankOrDifferentUser() = runTest {
        val fixed = entity("其他账号", userNo = "U2").copy(id = 5L)
        every { dao.getCourseById(5L) } returns MutableStateFlow(fixed)

        assertEquals(null, repo.observeCourseById(5L).first())
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        assertEquals(null, repo.observeCourseById(5L).first())
    }

    @Test
    fun insertCourseMapsToEntityAndReturnsId() = runTest {
        val captured = mutableListOf<CourseEntity>()
        everySuspend { dao.insertCourse(any()) } calls { (e: CourseEntity) ->
            captured.add(e)
            42L
        }
        val domain = entity("写入").toDomain().copy(semesterId = "S9", userNo = "U9")
        assertEquals(42L, repo.insertCourse(domain))
        assertEquals(domain, captured.single().toDomain())
    }

    @Test
    fun getAllCoursesOnceAlwaysUsesUserScope() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        everySuspend { dao.getCoursesByUserNoAndSemesterOnce("U1", "S1") } returns
            listOf(entity("once-semester"))
        everySuspend { dao.getCoursesByUserNoOnce("U1") } returns listOf(entity("once-user"))
        termFlow.value = "S1"
        assertEquals(listOf("once-semester"), repo.getAllCoursesOnce().map { it.name })
        termFlow.value = ""
        assertEquals(listOf("once-user"), repo.getAllCoursesOnce().map { it.name })
    }

    @Test
    fun getAllCoursesOnceBlankUserReturnsEmpty() = runTest {
        termFlow.value = "S1"
        assertTrue(repo.getAllCoursesOnce().isEmpty())
    }

    @Test
    fun getAllCoursesAcrossSemestersUsesUserAndIgnoresActiveTerm() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        everySuspend { dao.getCoursesByUserNoOnce("U1") } returns listOf(entity("across"))
        termFlow.value = "S1"
        assertEquals(listOf("across"), repo.getAllCoursesAcrossSemesters().map { it.name })
    }

    @Test
    fun getAllCoursesAcrossSemestersBlankUserReturnsEmpty() = runTest {
        assertTrue(repo.getAllCoursesAcrossSemesters().isEmpty())
    }

    @Test
    fun replaceSyncedCoursesScopesUserAndBindsManualCourses() = runTest {
        val capturedUsers = mutableListOf<String>()
        val capturedLists = mutableListOf<List<CourseEntity>>()
        val capturedSemesters = mutableListOf<String>()
        everySuspend { dao.replaceSyncedCourses(any(), any(), any()) } calls {
            (userNo: String, sem: String, list: List<CourseEntity>) ->
            capturedUsers.add(userNo)
            capturedSemesters.add(sem)
            capturedLists.add(list)
            Unit
        }

        every { settings.setActiveScheduleTerm(any()) } returns Unit
        repo.replaceSyncedCourses("U1", "S1", listOf(entity("x", userNo = "U2").toDomain()))
        verifySuspend { dao.replaceSyncedCourses("U1", "S1", any()) }
        verifySuspend { dao.bindManualCoursesWithoutSemester("U1", "S1") }
        verify { settings.setActiveScheduleTerm("S1") }
        assertEquals("U1", capturedUsers.single())
        assertEquals("S1", capturedSemesters.single())
        assertTrue(capturedLists.single().all { it.userNo == "U1" && it.semesterId == "S1" })
    }

    @Test
    fun replaceSyncedCoursesBlankSemesterSkipsBinding() = runTest {
        everySuspend { dao.replaceSyncedCourses(any(), any(), any()) } returns Unit
        repo.replaceSyncedCourses("U1", "", emptyList())
        verifySuspend { dao.replaceSyncedCourses("U1", "", emptyList()) }
    }

    @Test
    fun replaceSyncedCoursesRejectsBlankUser() = runTest {
        assertFailsWith<IllegalArgumentException> {
            repo.replaceSyncedCourses("", "S1", emptyList())
        }
    }

    @Test
    fun deleteCourseScopesToCurrentUser() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        repo.deleteCourse(entity("x").toDomain())
        verifySuspend { dao.deleteCourseByIdAndUserNo(1L, "U1") }
    }

    @Test
    fun deleteCourseRejectsBlankUser() = runTest {
        profileFlow.value = JwxtAuthProfile()
        assertFailsWith<IllegalArgumentException> {
            repo.deleteCourse(entity("x").toDomain())
        }
        verifySuspend(VerifyMode.not) { dao.deleteCourseByIdAndUserNo(any(), any()) }
    }
}
