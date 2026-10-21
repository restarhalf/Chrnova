package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
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
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FetchSemesterIdsUseCaseTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val settings = mock<TermSettingsPort>(MockMode.autofill)

    private val useCase = FetchSemesterIdsUseCase(authWorkflow, academic, settings)

    @Test
    fun `网络成功非空时返回并更新缓存`() = runTest {
        everySuspend { academic.fetchSemesterIds() } returns listOf("2026-1", "2025-2")

        val result = useCase()

        assertEquals(listOf("2026-1", "2025-2"), result)
        verify(VerifyMode.exactly(1)) { settings.setCachedSemesterIds(listOf("2026-1", "2025-2")) }
    }

    @Test
    fun `网络返回空时回退缓存`() = runTest {
        everySuspend { academic.fetchSemesterIds() } returns emptyList()
        every { settings.observeCachedSemesterIds() } returns flowOf(listOf("2025-2"))

        val result = useCase()

        assertEquals(listOf("2025-2"), result)
        verify(VerifyMode.not) { settings.setCachedSemesterIds(any()) }
    }

    @Test
    fun `网络失败时回退缓存`() = runTest {
        everySuspend { academic.fetchSemesterIds() } throws RuntimeException("no session")
        every { settings.observeCachedSemesterIds() } returns flowOf(listOf("2024-1"))

        val result = useCase()

        assertEquals(listOf("2024-1"), result)
    }

    @Test
    fun `网络失败且缓存为空时返回空列表`() = runTest {
        everySuspend { academic.fetchSemesterIds() } throws RuntimeException("no session")
        every { settings.observeCachedSemesterIds() } returns flowOf(emptyList())


        assertEquals(emptyList(), useCase())
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
    }

    @Test
    fun `刷新会话失败且缓存为空时抛出异常`() = runTest {
        everySuspend { academic.fetchSemesterIds() } throws RuntimeException("no session")
        everySuspend { authWorkflow.refreshSession() } throws RuntimeException("refresh failed")
        every { settings.observeCachedSemesterIds() } returns flowOf(emptyList())


        assertFailsWith<RuntimeException> { useCase() }
    }
}
