package restarhalf.stellar.schedule.data.impl

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.data.remote.JwxtCurriculumResponse
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtSync
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.JwxtSessionSnapshot
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncPortImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val curriculumJson = """
        {"code":"1","Msg":"",
         "data":[{"date":[{"xqmc":"一","mxrq":"2026-03-02","zc":"all","xqid":1}],
                  "item":[{"classTime":"30304","classWeekDetails":"1,3,5","classWeek":"",
                           "courseName":"高等数学","teacherName":"张三","location":"A101",
                           "kch":"MA101"}],
                  "week":"1","weekday":"三"}]}
    """.trimIndent().replace("\n", "")

    private val capturedFields = mutableListOf<Map<String, String>>()
    private val capturedUsers = mutableListOf<String>()
    private val capturedCourses = mutableListOf<List<Course>>()
    private val capturedSemesters = mutableListOf<String>()

    private val gateway = mock<JwxtGateway>(MockMode.autofill) {
        everySuspend { fetchCurriculum(any()) } calls { (fields: Map<String, String>) ->
            capturedFields.add(fields)
            json.decodeFromString(JwxtCurriculumResponse.serializer(), curriculumJson)
        }
    }

    private val courseRepository = mock<CourseRepository>(MockMode.autofill) {
        everySuspend { replaceSyncedCourses(any(), any(), any()) } calls {
            (userNo: String, semesterId: String, courses: List<Course>) ->
            capturedUsers.add(userNo)
            capturedSemesters.add(semesterId)
            capturedCourses.add(courses)
            Unit
        }
    }

    private var session = JwxtSessionSnapshot(userNo = "U1", generation = 0)
    private val auth = mock<JwxtAuthPort> {
        every { getSessionSnapshot() } calls { session }
    }

    private val impl = SyncPortImpl(JwxtSync(gateway), courseRepository, auth)

    @Test
    fun syncStampsSemesterAndUserNo() = runTest {
        val result = impl.sync(semesterId = "2026-2027-1", campusId = "CAMPUS-1", week = "all")

        assertEquals(
            mapOf("xnxq01id" to "2026-2027-1", "kbjcmsid" to "CAMPUS-1", "week" to "all"),
            capturedFields.single(),
        )

        val courses = capturedCourses.single()
        assertEquals(1, courses.size)
        val course = courses.single()
        assertEquals("高等数学", course.name)
        assertEquals("张三", course.teacher)
        assertEquals("A101", course.location)
        assertEquals(3, course.dayOfWeek)
        assertEquals(3, course.startSection)
        assertEquals(2, course.sectionCount)
        assertEquals(listOf(1, 3, 5), course.weeks)
        assertEquals("MA101|30304", course.remoteKey)
        assertEquals("2026-2027-1", course.semesterId)
        assertEquals("U1", course.userNo)
        assertEquals("U1", capturedUsers.single())

        assertEquals(
            SyncResult(inserted = 1, semesterId = "2026-2027-1", campusId = "CAMPUS-1", campusName = "", week = "all"),
            result,
        )
    }

    @Test
    fun syncRejectsBlankUserBeforeFetchingOrReplacing() = runTest {
        session = session.copy(userNo = "")

        val error = assertFailsWith<IllegalArgumentException> { impl.sync("S", "C", "all") }

        assertEquals("请先登录", error.message)
        assertTrue(capturedFields.isEmpty())
        assertTrue(capturedCourses.isEmpty())
    }

    @Test
    fun syncDiscardsResultWhenSessionChangesDuringRequest() = runTest {
        everySuspend { gateway.fetchCurriculum(any()) } calls {
            session = session.copy(generation = session.generation + 1)
            json.decodeFromString(JwxtCurriculumResponse.serializer(), curriculumJson)
        }

        val error = assertFailsWith<IllegalArgumentException> { impl.sync("S", "C", "all") }

        assertEquals("登录状态已变化，请重试", error.message)
        assertTrue(capturedCourses.isEmpty())
    }

    @Test
    fun gatewayFailurePropagates() = runTest {
        everySuspend { gateway.fetchCurriculum(any()) } returns
            json.decodeFromString(JwxtCurriculumResponse.serializer(), """{"code":"0","Msg":"boom"}""")
        val error = assertFailsWith<IllegalStateException> { impl.sync("S", "C", "all") }
        assertTrue(error.message!!.contains("boom"))
    }

    @Test
    fun fetchTermStartDateReturnsMondayMinusSevenDays() = runTest {
        val ms = impl.fetchTermStartDate("S", "C")

        val monday = kotlinx.datetime.LocalDate.parse("2026-03-02")
        val expected = monday.minus(7, DateTimeUnit.DAY)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        assertEquals(expected, ms)
    }

    @Test
    fun fetchTermStartDateReturnsNullWithoutMonday() = runTest {
        everySuspend { gateway.fetchCurriculum(any()) } returns
            json.decodeFromString(JwxtCurriculumResponse.serializer(), """{"code":"1","data":[]}""")
        assertNull(impl.fetchTermStartDate("S", "C"))
    }

    @Test
    fun fetchTermStartDateReturnsNullOnGatewayError() = runTest {
        everySuspend { gateway.fetchCurriculum(any()) } returns
            json.decodeFromString(JwxtCurriculumResponse.serializer(), """{"code":"0","Msg":"x"}""")
        assertNull(impl.fetchTermStartDate("S", "C"))
    }
}
