package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveAllUseCasesTest {

    private val examRepo = mock<ExaminationRepository>(MockMode.autofill)
    private val gradeRepo = mock<GradeRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `考试观察流按学号过滤`() = runTest {
        val bound = listOf(Examination(courseName = "高数"))
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        every { examRepo.observeExaminationsByUserNo("2023001") } returns flowOf(bound)

        val result = ObserveAllExaminationsUseCase(examRepo, auth)().drop(1).first()

        assertEquals(bound, result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `学号为空时不观察任何考试`() = runTest {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = ""))

        val result = ObserveAllExaminationsUseCase(examRepo, auth)().first()

        assertTrue(result.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `成绩观察流按学号过滤`() = runTest {
        val grades = listOf(GradeCourse(courseName = "高数"))
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        every { gradeRepo.observeGradesByUserNo("2023001") } returns flowOf(grades)

        val result = ObserveAllGradesUseCase(gradeRepo, auth)().drop(1).first()

        assertEquals(grades, result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `学号为空时不观察任何成绩`() = runTest {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = ""))

        val result = ObserveAllGradesUseCase(gradeRepo, auth)().first()

        assertTrue(result.isEmpty())
    }
}
