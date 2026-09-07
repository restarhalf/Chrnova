package restarhalf.stellar.schedule.data.repository

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.local.dao.CourseDao
import restarhalf.stellar.schedule.data.local.entity.CourseEntity
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomCourseRepositoryTest {

    private val termFlow = MutableStateFlow("")
    private val profileFlow = MutableStateFlow(JwxtAuthProfile())

    private fun entity(marker: String) = CourseEntity(
        id = 1L,
        name = marker,
        location = "L",
        teacher = "T",
        dayOfWeek = 1,
        startSection = 1,
        sectionCount = 1,
        weeks = listOf(1),
        color = "#000000",
    )

    private val dao = mock<CourseDao>(MockMode.autofill) {
        // 四条观察路径各给一个可区分的标记课程
        every { getAllCourses() } returns MutableStateFlow(listOf(entity("all")))
        every { getCoursesByUserNo(any()) } returns MutableStateFlow(listOf(entity("byUser")))
        every { getCoursesBySemester(any()) } returns MutableStateFlow(listOf(entity("bySemester")))
        every { getCoursesByUserNoAndSemester(any(), any()) } returns
            MutableStateFlow(listOf(entity("byUserAndSemester")))
        every { getCourseById(any()) } returns MutableStateFlow<CourseEntity?>(null)
    }

    private val settings = mock<SettingsPort> {
        every { observeActiveScheduleTerm() } returns termFlow
    }
    private val auth = mock<JwxtAuthPort> {
        every { observeProfile() } returns profileFlow
    }

    private val repo = RoomCourseRepository(dao, settings, auth)

    // ---------- observeAllCourses 的四条过滤路径 ----------

    @Test
    fun observeAllBothBlankUsesAllCourses() = runTest {
        val names = repo.observeAllCourses().first().map { it.name }
        assertEquals(listOf("all"), names)
    }

    @Test
    fun observeAllUserOnlyUsesUserNo() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        val names = repo.observeAllCourses().first().map { it.name }
        assertEquals(listOf("byUser"), names)
    }

    @Test
    fun observeAllSemesterOnlyUsesSemester() = runTest {
        termFlow.value = "S1"
        val names = repo.observeAllCourses().first().map { it.name }
        assertEquals(listOf("bySemester"), names)
    }

    @Test
    fun observeAllUserAndSemesterUsesCombined() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "U1")
        termFlow.value = "S1"
        val names = repo.observeAllCourses().first().map { it.name }
        assertEquals(listOf("byUserAndSemester"), names)
    }

    @Test
    fun observeAllMapsEntityToDomain() = runTest {
        val course = repo.observeAllCourses().first().single()
        assertEquals(1L, course.id)
        assertTrue(course is Course)
    }

    // ---------- observeCourseById ----------

    @Test
    fun observeCourseByIdFilteredByActiveSemester() = runTest {
        val fixed = CourseEntity(
            id = 5L, name = "定课", semesterId = "S1",
            location = "L", teacher = "T", dayOfWeek = 1, startSection = 1,
            sectionCount = 1, weeks = listOf(1), color = "#000000",
        )
        every { dao.getCourseById(5L) } returns MutableStateFlow(fixed)

        // 激活学期为空 → 不过滤
        assertEquals("定课", repo.observeCourseById(5L).first()?.name)
        // 激活学期匹配 → 保留
        termFlow.value = "S1"
        assertEquals("定课", repo.observeCourseById(5L).first()?.name)
        // 激活学期不匹配 → 过滤为 null
        termFlow.value = "S2"
        assertEquals(null, repo.observeCourseById(5L).first())
    }

    // ---------- 写路径 ----------

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
    fun getAllCoursesOnceUsesSemesterQueryWhenActive() = runTest {
        everySuspend { dao.getCoursesBySemesterOnce("S1") } returns listOf(entity("once-semester"))
        everySuspend { dao.getAllCoursesOnce() } returns listOf(entity("once-all"))
        termFlow.value = "S1"
        assertEquals(listOf("once-semester"), repo.getAllCoursesOnce().map { it.name })
        termFlow.value = ""
        assertEquals(listOf("once-all"), repo.getAllCoursesOnce().map { it.name })
    }

    @Test
    fun getAllCoursesAcrossSemestersIgnoresActiveTerm() = runTest {
        everySuspend { dao.getAllCoursesOnce() } returns listOf(entity("across"))
        termFlow.value = "S1"
        assertEquals(listOf("across"), repo.getAllCoursesAcrossSemesters().map { it.name })
    }

    @Test
    fun replaceSyncedCoursesBindsManualAndSetsActiveTerm() = runTest {
        val capturedLists = mutableListOf<List<CourseEntity>>()
        val capturedSemesters = mutableListOf<String>()
        everySuspend { dao.replaceSyncedCourses(any(), any()) } calls { (list: List<CourseEntity>, sem: String) ->
            capturedLists.add(list)
            capturedSemesters.add(sem)
            Unit
        }
        // settings 为 strict mock：写路径会调用 setActiveScheduleTerm，需显式 stub
        every { settings.setActiveScheduleTerm(any()) } returns Unit
        repo.replaceSyncedCourses(listOf(entity("x").toDomain()), "S1")
        verifySuspend { dao.replaceSyncedCourses(any(), any()) }
        verifySuspend { dao.bindManualCoursesWithoutSemester("S1") }
        verify { settings.setActiveScheduleTerm("S1") }
        assertEquals("S1", capturedSemesters.single())
    }

    @Test
    fun replaceSyncedCoursesBlankSemesterSkipsBinding() = runTest {
        everySuspend { dao.replaceSyncedCourses(any(), any()) } returns Unit
        repo.replaceSyncedCourses(emptyList(), "")
        verifySuspend { dao.replaceSyncedCourses(emptyList(), "") }
    }

    @Test
    fun clearAllCoursesDeletesAndResetsTerm() = runTest {
        everySuspend { dao.deleteAll() } returns Unit
        every { settings.setActiveScheduleTerm(any()) } returns Unit
        termFlow.value = "S1"
        repo.clearAllCourses()
        verifySuspend { dao.deleteAll() }
        verify { settings.setActiveScheduleTerm("") }
    }

    @Test
    fun bindUnboundCoursesDelegates() = runTest {
        everySuspend { dao.bindUnboundCourses("U7") } returns Unit
        repo.bindUnboundCourses("U7")
        verifySuspend { dao.bindUnboundCourses("U7") }
    }
}
