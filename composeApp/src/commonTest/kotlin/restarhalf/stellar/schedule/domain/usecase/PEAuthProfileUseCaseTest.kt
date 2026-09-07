package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.remote.PEAuthProfile as PEAuthProfileDto
import restarhalf.stellar.schedule.data.remote.PEAuthProfileResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import kotlin.test.Test
import kotlin.test.assertEquals

class PEAuthProfileUseCaseTest {

    private val gateway = mock<PEGateway>(MockMode.autofill)
    private val auth = mock<PEAuthPort>(MockMode.autofill)
    private val authWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)
    private val useCase = PEAuthProfileUseCase(gateway, auth, authWorkflow)

    @Test
    fun `获取学生信息并写入档案`() = runTest {
        everySuspend { gateway.getProfile() } returns PEAuthProfileResponse(
            data = PEAuthProfileDto(testCode = "T01", stuName = "张三", stdNumber = "2023001")
        )

        val response = useCase()

        assertEquals("张三", response.data?.stuName)
        verify(VerifyMode.exactly(1)) {
            auth.setProfile(PEAuthProfile(stuName = "张三", stdNumber = "2023001", testCode = "T01"))
        }
    }

    @Test
    fun `响应无数据时不写档案`() = runTest {
        everySuspend { gateway.getProfile() } returns PEAuthProfileResponse(status = "ok")

        val response = useCase()

        assertEquals(null, response.data)
        verify(VerifyMode.not) { auth.setProfile(any()) }
    }

    @Test
    fun `令牌过期时刷新会话并重试`() = runTest {
        var attempts = 0
        everySuspend { gateway.getProfile() } calls {
            attempts++
            if (attempts == 1) throw PETokenExpiredException()
            PEAuthProfileResponse(data = PEAuthProfileDto(stuName = "李四"))
        }

        val response = useCase()

        assertEquals("李四", response.data?.stuName)
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
    }
}
