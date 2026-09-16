package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.RemoteCampus
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.ReminderSchedulerPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RunSyncUseCaseTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val sync = mock<SyncPort>(MockMode.autofill)
    private val reminderScheduler = mock<ReminderSchedulerPort>(MockMode.autofill)

    private val useCase = RunSyncUseCase(
        authWorkflow = authWorkflow,
        academic = academic,
        timetable = timetable,
        settings = settings,
        sync = sync,
        reminderScheduler = reminderScheduler,
    )

    private val jinshitan = RemoteCampus(id = "c-js", name = "金石滩校区", isDefault = false)
    private val development = RemoteCampus(id = "c-kf", name = "开发区校区", isDefault = false)

    private fun syncResult(inserted: Int, campusId: String) = SyncResult(
        inserted = inserted,
        semesterId = "2026-1",
        campusId = campusId,
        campusName = "",
        week = "all",
    )

    private fun stubHappyPath() {
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        every { timetable.getCampus() } returns Campus.Jinshitan
        everySuspend { academic.fetchCampuses() } returns listOf(jinshitan, development)
        everySuspend { sync.sync(any(), any(), any()) } returns syncResult(5, "c-js")
        everySuspend { sync.fetchTermStartDate(any(), any()) } returns 1000L
        everySuspend { academic.fetchTeachingWeekTotal() } returns 20
    }

    @Test
    fun `主流程匹配本地校区并写入学期开始与总周数`() = runTest {
        stubHappyPath()

        val result = useCase()

        assertEquals(5, result.inserted)
        assertEquals("金石滩校区", result.campusName)
        verifySuspend(VerifyMode.exactly(1)) { sync.sync("2026-1", "c-js", "all") }
        verify(VerifyMode.exactly(1)) { timetable.setTermStartMs(1000L) }
        verify(VerifyMode.exactly(1)) { timetable.setTotalWeeks(20) }
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.ensureLoggedIn() }
        verify(VerifyMode.exactly(1)) { reminderScheduler.scheduleNow() }
    }

    @Test
    fun `无选中学期时回退教务当前学期`() = runTest {
        every { settings.observeSelectedTerm() } returns flowOf("")
        everySuspend { academic.fetchCurrentTermId() } returns "2026-2"
        every { timetable.getCampus() } returns Campus.Jinshitan
        everySuspend { academic.fetchCampuses() } returns listOf(jinshitan)
        everySuspend { sync.sync(any(), any(), any()) } returns syncResult(1, "c-js")
        everySuspend { sync.fetchTermStartDate(any(), any()) } returns null
        everySuspend { academic.fetchTeachingWeekTotal() } returns 0

        useCase()

        verifySuspend(VerifyMode.exactly(1)) { sync.sync("2026-2", "c-js", "all") }
        verify(VerifyMode.not) { timetable.setTermStartMs(any()) }
        verify(VerifyMode.not) { timetable.setTotalWeeks(any()) }
    }

    @Test
    fun `校区列表为空时抛出IllegalStateException`() = runTest {
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        every { timetable.getCampus() } returns Campus.Jinshitan
        everySuspend { academic.fetchCampuses() } returns emptyList()

        assertFailsWith<IllegalStateException> { useCase() }
        verifySuspend(VerifyMode.not) { sync.sync(any(), any(), any()) }
        verify(VerifyMode.not) { reminderScheduler.scheduleNow() }
    }

    @Test
    fun `名称不匹配时回退默认校区`() = runTest {
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        every { timetable.getCampus() } returns Campus.Jinshitan
        val mainCampus = RemoteCampus(id = "c-main", name = "主校区", isDefault = true)
        everySuspend { academic.fetchCampuses() } returns listOf(mainCampus, development)
        everySuspend { sync.sync(any(), any(), any()) } returns syncResult(2, "c-main")
        everySuspend { sync.fetchTermStartDate(any(), any()) } returns null
        everySuspend { academic.fetchTeachingWeekTotal() } returns 0

        val result = useCase()

        assertEquals("主校区", result.campusName)
        verifySuspend(VerifyMode.exactly(1)) { sync.sync("2026-1", "c-main", "all") }
    }

    @Test
    fun `主校区0课程时切备用校区成功并切换本地校区`() = runTest {
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        every { timetable.getCampus() } returns Campus.Jinshitan
        everySuspend { academic.fetchCampuses() } returns listOf(jinshitan, development)
        var attempts = 0
        everySuspend { sync.sync(any(), any(), any()) } calls { (_: String, campusId: String, _: String) ->
            attempts++
            if (attempts == 1) syncResult(0, campusId) else syncResult(3, campusId)
        }
        everySuspend { sync.fetchTermStartDate(any(), any()) } returns null
        everySuspend { academic.fetchTeachingWeekTotal() } returns 0

        val result = useCase()

        assertEquals(2, attempts)
        assertEquals(3, result.inserted)
        assertEquals("开发区校区", result.campusName)
        verify(VerifyMode.exactly(1)) { timetable.setCampus(Campus.Development) }
    }

    @Test
    fun `备用校区也为0课程时保留原校区`() = runTest {
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        every { timetable.getCampus() } returns Campus.Jinshitan
        everySuspend { academic.fetchCampuses() } returns listOf(jinshitan, development)
        everySuspend { sync.sync(any(), any(), any()) } returns syncResult(0, "c-js")
        everySuspend { sync.fetchTermStartDate(any(), any()) } returns null
        everySuspend { academic.fetchTeachingWeekTotal() } returns 0

        val result = useCase()

        assertEquals(0, result.inserted)
        assertEquals("金石滩校区", result.campusName)
        verify(VerifyMode.not) { timetable.setCampus(any()) }
    }

    @Test
    fun `网络错误直抛不刷新会话`() = runTest {
        stubHappyPath()
        everySuspend { sync.sync(any(), any(), any()) } throws RuntimeException("connection timeout")

        assertFailsWith<RuntimeException> { useCase() }
        verifySuspend(VerifyMode.not) { authWorkflow.refreshSession() }
        verify(VerifyMode.not) { reminderScheduler.scheduleNow() }
    }

    @Test
    fun `非网络错误刷新会话后重试成功`() = runTest {
        stubHappyPath()
        var attempts = 0
        everySuspend { sync.sync(any(), any(), any()) } calls {
            attempts++
            if (attempts == 1) throw RuntimeException("no session")
            syncResult(4, "c-js")
        }

        val result = useCase()

        assertEquals(4, result.inserted)
        assertEquals(2, attempts)
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
        verify(VerifyMode.exactly(1)) { reminderScheduler.scheduleNow() }
    }
}
