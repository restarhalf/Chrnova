package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ExaminationUseCasesTest {

    private val repository = mock<ExaminationRepository>(MockMode.autofill)

    @Test
    fun `删除考试透传ID到repository`() = runTest {
        val useCase = DeleteExaminationUseCase(repository)
        useCase(42L)
        verifySuspend(VerifyMode.exactly(1)) { repository.deleteExamination(42L) }
    }

    @Test
    fun `保存考试透传参数并返回行ID`() = runTest {
        val exam = Examination(
            courseNumber = "C001",
            courseName = "高等数学",
            time = "2026-01-15 14:00-16:00",
            examinationPlace = "教学楼A101",
        )
        everySuspend { repository.saveExamination(any(), any()) } returns 77L
        val useCase = SaveExaminationUseCase(repository)

        val rowId = useCase(exam, "2026-1")

        assertEquals(77L, rowId)
        verifySuspend(VerifyMode.exactly(1)) { repository.saveExamination(exam, "2026-1") }
    }
}
