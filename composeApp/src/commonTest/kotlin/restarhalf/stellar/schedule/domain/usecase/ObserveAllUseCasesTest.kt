package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

        val result = ObserveAllExaminationsUseCase(examRepo, auth)().first()

        assertEquals(bound, result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `学号为空时观察全部考试`() = runTest {
        val all = listOf(Examination(courseName = "英语"))
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = ""))
        every { examRepo.observeAllExaminations() } returns flowOf(all)

        val result = ObserveAllExaminationsUseCase(examRepo, auth)().first()

        assertEquals(all, result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `成绩观察流按学号过滤`() = runTest {
        val grades = listOf(GradeCourse(courseName = "高数"))
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        every { gradeRepo.observeGradesByUserNo("2023001") } returns flowOf(grades)

        val result = ObserveAllGradesUseCase(gradeRepo, auth)().first()

        assertEquals(grades, result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `学号为空时观察全部成绩`() = runTest {
        val grades = listOf(GradeCourse(courseName = "英语"))
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = ""))
        every { gradeRepo.observeAllGrades() } returns flowOf(grades)

        val result = ObserveAllGradesUseCase(gradeRepo, auth)().first()

        assertEquals(grades, result)
    }
}
