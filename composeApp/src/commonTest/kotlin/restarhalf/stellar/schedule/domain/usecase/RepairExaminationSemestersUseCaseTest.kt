package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlin.test.Test

class RepairExaminationSemestersUseCaseTest {

    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)

    private val useCase = RepairExaminationSemestersUseCase(examinationRepository, academic)

    @Test
    fun `修复无效考试学期ID`() = runTest {
        everySuspend { academic.fetchCurrentTermId() } returns "2026-1"

        useCase()

        verifySuspend(VerifyMode.exactly(1)) { examinationRepository.fixInvalidSemesterIds("2026-1") }
    }

    @Test
    fun `获取当前学期失败时静默完成`() = runTest {
        everySuspend { academic.fetchCurrentTermId() } throws RuntimeException("no session")

        useCase()

        verifySuspend(VerifyMode.not) { examinationRepository.fixInvalidSemesterIds(any()) }
    }

    @Test
    fun `当前学期ID为空时跳过修复`() = runTest {
        everySuspend { academic.fetchCurrentTermId() } returns ""

        useCase()

        verifySuspend(VerifyMode.not) { examinationRepository.fixInvalidSemesterIds(any()) }
    }
}
