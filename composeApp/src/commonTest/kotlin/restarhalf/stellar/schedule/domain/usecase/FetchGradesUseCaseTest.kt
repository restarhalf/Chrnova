package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FetchGradesUseCaseTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val repository = mock<GradeRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)

    private val useCase = FetchGradesUseCase(authWorkflow, academic, settings, repository, auth)

    private fun grade(semester: String = "2026-1") = GradeCourse(
        courseCode = "X1A001",
        courseName = "高等数学",
        score = "85",
        credit = 2.0,
        semester = semester,
    )

    private fun report(vararg grades: GradeCourse) = TermGradeReport(achievements = grades.toList())

    private fun stubProfile(userNo: String) {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = userNo))
    }

    @Test
    fun `有成绩时直接返回并落库`() = runTest {
        stubProfile("2023001")
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        everySuspend { academic.fetchGradeReport(any()) } returns report(grade())

        val result = useCase()

        assertEquals(1, result.achievements.size)
        verifySuspend(VerifyMode.exactly(1)) { academic.fetchGradeReport("2026-1") }
        verifySuspend(VerifyMode.exactly(1)) {
            repository.replaceGradesByUserNoAndSemester("2023001", "2026-1", result.achievements.map { it.copy(userNo = "2023001") })
        }
    }

    @Test
    fun `当前学期无成绩回退上一学期`() = runTest {
        stubProfile("2023001")
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        everySuspend { academic.fetchSemesterIds() } returns listOf("2025-2", "2026-1")
        everySuspend { academic.fetchGradeReport("2026-1") } returns report()
        everySuspend { academic.fetchGradeReport("2025-2") } returns report(grade("2025-2"))

        val result = useCase()

        assertEquals(1, result.achievements.size)
        assertEquals("2025-2", result.achievements[0].semester)
    }

    @Test
    fun `当前学期不在学期列表时不回退`() = runTest {
        stubProfile("2023001")
        every { settings.observeSelectedTerm() } returns flowOf("2099-9")
        everySuspend { academic.fetchSemesterIds() } returns listOf("2025-2", "2026-1")
        everySuspend { academic.fetchGradeReport(any()) } returns report()

        val result = useCase()

        assertEquals(0, result.achievements.size)
        verifySuspend(VerifyMode.exactly(1)) { academic.fetchGradeReport("2099-9") }
        verifySuspend(VerifyMode.not) { repository.replaceGradesByUserNoAndSemester(any(), any(), any()) }
    }

    @Test
    fun `网络错误直接抛出不重试`() = runTest {
        stubProfile("2023001")
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        everySuspend { academic.fetchGradeReport(any()) } throws RuntimeException("connection timeout")

        assertFailsWith<RuntimeException> { useCase() }
        verifySuspend(VerifyMode.not) { authWorkflow.refreshSession() }
    }

    @Test
    fun `非网络错误刷新会话重试成功`() = runTest {
        stubProfile("2023001")
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        var attempts = 0
        everySuspend { academic.fetchGradeReport(any()) } calls { (_: String) ->
            attempts++
            if (attempts == 1) throw RuntimeException("no session")
            report(grade())
        }

        val result = useCase()

        assertEquals(2, attempts)
        assertEquals(1, result.achievements.size)
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
    }

    @Test
    fun `Simple 包装透传结果`() = runTest {
        stubProfile("2023001")
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        everySuspend { academic.fetchGradeReport(any()) } returns report(grade())
        // FetchGradesUseCase 是 final class 无法 mock，用真实实例走完整委托链
        val simple = FetchGradesSimpleUseCase(
            FetchGradesUseCase(authWorkflow, academic, settings, repository, auth)
        )

        val result = simple("2026-1")

        assertEquals(1, result.achievements.size)
        verifySuspend(VerifyMode.exactly(1)) { academic.fetchGradeReport("2026-1") }
    }
}
