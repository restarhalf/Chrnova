package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlin.test.Test

class BindUnboundDataUseCaseTest {

    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)

    private val useCase = BindUnboundDataUseCase(auth, courseRepository, examinationRepository, academic)

    @Test
    fun `已登录时绑定课程考试并修复学期ID`() = runTest {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        everySuspend { academic.fetchCurrentTermId() } returns "2026-1"

        useCase()

        verifySuspend(VerifyMode.exactly(1)) { courseRepository.bindUnboundCourses("2023001") }
        verifySuspend(VerifyMode.exactly(1)) { examinationRepository.bindUnboundExaminations("2023001") }
        verifySuspend(VerifyMode.exactly(1)) { examinationRepository.fixInvalidSemesterIds("2026-1") }
    }

    @Test
    fun `未登录时跳过绑定`() = runTest {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = ""))

        useCase()

        verifySuspend(VerifyMode.not) { courseRepository.bindUnboundCourses(any()) }
        verifySuspend(VerifyMode.not) { examinationRepository.bindUnboundExaminations(any()) }
    }

    @Test
    fun `档案流为空时跳过绑定`() = runTest {
        every { auth.observeProfile() } returns emptyFlow()

        useCase()

        verifySuspend(VerifyMode.not) { courseRepository.bindUnboundCourses(any()) }
    }

    @Test
    fun `获取当前学期失败时吞掉异常但不影响绑定`() = runTest {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        everySuspend { academic.fetchCurrentTermId() } throws RuntimeException("no session")

        useCase()

        verifySuspend(VerifyMode.exactly(1)) { courseRepository.bindUnboundCourses("2023001") }
        verifySuspend(VerifyMode.exactly(1)) { examinationRepository.bindUnboundExaminations("2023001") }
        verifySuspend(VerifyMode.not) { examinationRepository.fixInvalidSemesterIds(any()) }
    }

    @Test
    fun `当前学期ID为空时跳过修复`() = runTest {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        everySuspend { academic.fetchCurrentTermId() } returns ""

        useCase()

        verifySuspend(VerifyMode.exactly(1)) { courseRepository.bindUnboundCourses("2023001") }
        verifySuspend(VerifyMode.not) { examinationRepository.fixInvalidSemesterIds(any()) }
    }
}
