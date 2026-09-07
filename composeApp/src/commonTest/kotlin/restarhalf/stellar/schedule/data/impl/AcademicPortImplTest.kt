package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.remote.JwxtApiResponse
import restarhalf.stellar.schedule.data.remote.JwxtCampusItem
import restarhalf.stellar.schedule.data.remote.JwxtCampusResponse
import restarhalf.stellar.schedule.data.remote.JwxtCurrentTermItem
import restarhalf.stellar.schedule.data.remote.JwxtExaminationItem
import restarhalf.stellar.schedule.data.remote.JwxtExaminationResponse
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtSemesterListItem
import restarhalf.stellar.schedule.data.remote.JwxtTeachingWeekItem
import restarhalf.stellar.schedule.data.remote.JwxtTeachingWeekResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcademicPortImplTest {

    private val gateway = mock<JwxtGateway>(MockMode.autofill)
    private val settings = SettingsPortImpl(MapSettings())
    private val impl = AcademicPortImpl(gateway, settings)

    // ---------- fetchCurrentTermId ----------

    @Test
    fun fetchCurrentTermIdSuccessCaches() = runTest {
        everySuspend { gateway.getCurrentTerm() } returns
            JwxtApiResponse<List<JwxtCurrentTermItem>>(code = "1", data = listOf(JwxtCurrentTermItem(semesterId = "2026-2027-1")))
        assertEquals("2026-2027-1", impl.fetchCurrentTermId())
        // 已写入缓存
        assertEquals("2026-2027-1", settings.observeCurrentTermId().first())
    }

    @Test
    fun fetchCurrentTermIdFallsBackToCacheOnFailure() = runTest {
        settings.setCurrentTermId("CACHED-TERM")
        everySuspend { gateway.getCurrentTerm() } returns
            JwxtApiResponse<List<JwxtCurrentTermItem>>(code = "0", msg = "network down")
        assertEquals("CACHED-TERM", impl.fetchCurrentTermId())
    }

    @Test
    fun fetchCurrentTermIdThrowsWhenNoCacheAndFailure() = runTest {
        everySuspend { gateway.getCurrentTerm() } returns
            JwxtApiResponse<List<JwxtCurrentTermItem>>(code = "0", msg = "boom")
        val error = assertFailsWith<IllegalStateException> { impl.fetchCurrentTermId() }
        assertTrue(error.message!!.contains("boom"))
    }

    @Test
    fun fetchCurrentTermIdThrowsWhenDataEmpty() = runTest {
        everySuspend { gateway.getCurrentTerm() } returns
            JwxtApiResponse<List<JwxtCurrentTermItem>>(code = "1", data = emptyList())
        assertFailsWith<IllegalStateException> { impl.fetchCurrentTermId() }
    }

    // ---------- fetchCampuses ----------

    @Test
    fun fetchCampusesMapsDefaultFlag() = runTest {
        everySuspend { gateway.getCampusList() } returns JwxtCampusResponse(
            code = 1,
            data = listOf(
                JwxtCampusItem(isDefault = "1", kbjcmsid = "ID-DEV", kbjcmsmc = "开发区校区"),
                JwxtCampusItem(isDefault = "", kbjcmsid = "ID-JST", kbjcmsmc = "金石滩校区"),
            ),
        )
        val campuses = impl.fetchCampuses()
        assertEquals(2, campuses.size)
        assertEquals("ID-DEV", campuses[0].id)
        assertEquals("开发区校区", campuses[0].name)
        assertTrue(campuses[0].isDefault)
        assertEquals("ID-JST", campuses[1].id)
        assertFalse(campuses[1].isDefault)
    }

    @Test
    fun fetchCampusesThrowsOnFailure() = runTest {
        everySuspend { gateway.getCampusList() } returns JwxtCampusResponse(code = 0, msg = "campus err")
        val error = assertFailsWith<IllegalStateException> { impl.fetchCampuses() }
        assertTrue(error.message!!.contains("campus err"))
    }

    // ---------- fetchSemesterIds ----------

    @Test
    fun fetchSemesterIdsDedupAndFilterBlank() = runTest {
        everySuspend { gateway.getSemesterListFromEndpoint() } returns listOf(
            JwxtSemesterListItem(semesterId = "S1"),
            JwxtSemesterListItem(semesterId = ""),
            JwxtSemesterListItem(semesterId = "S1"),
            JwxtSemesterListItem(semesterId = "S2"),
        )
        assertEquals(listOf("S1", "S2"), impl.fetchSemesterIds())
    }

    // ---------- fetchExaminations ----------

    @Test
    fun fetchExaminationsMapsFields() = runTest {
        everySuspend { gateway.fetchExaminationArrangement(any(), any()) } returns JwxtExaminationResponse(
            code = "1",
            data = listOf(
                JwxtExaminationItem(
                    courseNumber = "MA101",
                    courseName = "高等数学",
                    time = "2026-01-15 14:00-16:00",
                    examinationPlace = "考场三",
                    zwh = "18",
                    ksbz = "正常",
                ),
            ),
        )
        val exams = impl.fetchExaminations("S1", "数学")
        assertEquals(1, exams.size)
        val exam = exams.single()
        assertEquals("MA101", exam.courseNumber)
        assertEquals("高等数学", exam.courseName)
        assertEquals("2026-01-15 14:00-16:00", exam.time)
        assertEquals("考场三", exam.examinationPlace)
        assertEquals("18", exam.zwh)
        assertEquals("正常", exam.ksbz)
        // 默认来源为教务同步
        assertEquals("sync", exam.source)
    }

    @Test
    fun fetchExaminationsThrowsOnFailure() = runTest {
        everySuspend { gateway.fetchExaminationArrangement(any(), any()) } returns
            JwxtExaminationResponse(code = "0", msg = "exam err")
        val error = assertFailsWith<IllegalStateException> { impl.fetchExaminations("S1", "") }
        assertTrue(error.message!!.contains("exam err"))
    }

    // ---------- fetchTeachingWeekTotal ----------

    @Test
    fun teachingWeekTotalCountsData() = runTest {
        everySuspend { gateway.getTeachingWeek() } returns JwxtTeachingWeekResponse(
            code = "1",
            data = listOf(JwxtTeachingWeekItem(), JwxtTeachingWeekItem(), JwxtTeachingWeekItem()),
        )
        assertEquals(3, impl.fetchTeachingWeekTotal())
    }

    @Test
    fun teachingWeekTotalZeroOnException() = runTest {
        everySuspend { gateway.getTeachingWeek() } calls {
            throw RuntimeException("offline")
        }
        assertEquals(0, impl.fetchTeachingWeekTotal())
    }

    @Test
    fun teachingWeekTotalZeroOnFailureCode() = runTest {
        everySuspend { gateway.getTeachingWeek() } returns JwxtTeachingWeekResponse(code = "0")
        assertEquals(0, impl.fetchTeachingWeekTotal())
    }
}
