package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
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

class FetchExaminationsSimpleUseCaseTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val repository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    // FetchExaminationsUseCase 是 final class 无法 mock，用真实实例走完整委托链
    private val simple = FetchExaminationsSimpleUseCase(
        FetchExaminationsUseCase(authWorkflow, academic, repository, auth, settings)
    )

    @Test
    fun `Simple 包装透传参数与结果`() = runTest {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        everySuspend { academic.fetchExaminations(any(), any()) }
            .returns(listOf(Examination(courseName = "高等数学")))

        val result = simple(semester = "2026-1", nameOrNumber = "数学")

        assertEquals(1, result.size)
        assertEquals("高等数学", result[0].courseName)
        verifySuspend(VerifyMode.exactly(1)) { academic.fetchExaminations("2026-1", "数学") }
    }
}
