package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.calls
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FetchExaminationsUseCaseTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val repository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    private val useCase = FetchExaminationsUseCase(authWorkflow, academic, repository, auth, settings)

    private val exam = Examination(courseNumber = "C001", courseName = "高等数学", time = "2026-01-15 14:00-16:00")

    private fun stubProfile(userNo: String) {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = userNo))
    }

    @Test
    fun `显式学期与筛选参数直接透传且不落库`() = runTest {
        stubProfile("2023001")
        everySuspend { academic.fetchExaminations(any(), any()) } returns listOf(exam)

        val result = useCase(semester = "2026-1", nameOrNumber = "数学")

        assertEquals(1, result.size)
        verifySuspend(VerifyMode.exactly(1)) { academic.fetchExaminations("2026-1", "数学") }
        verifySuspend(VerifyMode.not) { repository.replaceExaminations(any(), any()) }
    }

    @Test
    fun `默认参数解析选中学期与学号并替换本地库`() = runTest {
        stubProfile("2023001")
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        everySuspend { academic.fetchExaminations(any(), any()) } returns listOf(exam)

        val result = useCase()

        assertEquals(1, result.size)
        // 学号绑定到结果
        assertEquals("2023001", result[0].userNo)
        verifySuspend(VerifyMode.exactly(1)) { academic.fetchExaminations("2026-1", "2023001") }
        verifySuspend(VerifyMode.exactly(1)) { repository.replaceExaminations("2026-1", result) }
    }

    @Test
    fun `无选中学期时回退教务当前学期`() = runTest {
        stubProfile("2023001")
        every { settings.observeSelectedTerm() } returns flowOf("")
        everySuspend { academic.fetchCurrentTermId() } returns "2026-2"
        everySuspend { academic.fetchExaminations(any(), any()) } returns listOf(exam)

        useCase()

        verifySuspend(VerifyMode.exactly(1)) { academic.fetchExaminations("2026-2", "2023001") }
    }

    @Test
    fun `非网络错误时刷新会话后重试`() = runTest {
        stubProfile("2023001")
        var attempts = 0
        everySuspend { academic.fetchExaminations(any(), any()) } calls { (_: String, _: String) ->
            attempts++
            if (attempts == 1) throw RuntimeException("no session")
            listOf(exam)
        }

        val result = useCase(semester = "2026-1", nameOrNumber = "数学")

        assertEquals(2, attempts)
        assertEquals(listOf("高等数学"), result.map { it.courseName })
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
    }

    @Test
    fun `网络错误直接抛出不重试`() = runTest {
        stubProfile("2023001")
        everySuspend { academic.fetchExaminations(any(), any()) } throws RuntimeException("connection timeout")

        assertFailsWith<RuntimeException> { useCase(semester = "2026-1", nameOrNumber = "数学") }
        verifySuspend(VerifyMode.not) { authWorkflow.refreshSession() }
    }
}
