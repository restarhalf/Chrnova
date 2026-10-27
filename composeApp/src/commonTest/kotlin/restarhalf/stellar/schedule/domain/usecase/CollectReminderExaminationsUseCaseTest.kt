package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectReminderExaminationsUseCaseTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val repository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val settings = mock<TermSettingsPort>(MockMode.autofill)

    private val useCase = CollectReminderExaminationsUseCase(
        fetchExaminations = FetchExaminationsUseCase(
            authWorkflow, academic, repository, auth, settings,
        ),
        repository = repository,
    )

    private val remoteExam =
        Examination(
            courseName = "高等数学",
            time = "2026-01-15 14:00-16:00",
            examinationPlace = "A101",
            source = "sync",
        )
    private val manualExam =
        Examination(
            courseName = "自建选修",
            time = "2026-01-20 09:00-11:00",
            examinationPlace = "B202",
            source = "manual",
            id = 7,
        )

    private fun stubProfile() {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        every { settings.observeSelectedTerm() } returns flowOf("2026-1")
        everySuspend { authWorkflow.ensureLoggedIn() } returns Unit
    }

    @Test
    fun `教务与本地库合并，手动添加的也在里面`() = runTest {
        stubProfile()
        everySuspend { academic.fetchExaminations(any(), any()) } returns listOf(remoteExam)
        everySuspend { repository.getAllExaminationsOnce() } returns listOf(remoteExam, manualExam)

        val result = useCase()

        assertFalse(result.remoteFailed)
        assertEquals(2, result.exams.size)
        assertTrue(result.exams.any { it.courseName == "自建选修" })
    }

    @Test
    fun `教务拉不到时退化成只用本地库`() = runTest {
        stubProfile()
        everySuspend { academic.fetchExaminations(any(), any()) } throws IllegalStateException("boom")
        everySuspend { repository.getAllExaminationsOnce() } returns listOf(manualExam)

        val result = useCase()

        assertTrue(result.remoteFailed)
        assertEquals(listOf(manualExam), result.exams)
    }

    @Test
    fun `两边同时有同一场考试时只保留一条`() = runTest {
        stubProfile()
        everySuspend { academic.fetchExaminations(any(), any()) } returns listOf(remoteExam)
        everySuspend { repository.getAllExaminationsOnce() } returns
            listOf(remoteExam.copy(id = 3), manualExam)

        val result = useCase()

        assertEquals(2, result.exams.size)
        // 远程的排在前面，保留的是刚拉到的那条
        assertEquals(0L, result.exams.first { it.courseName == "高等数学" }.id)
    }
}
