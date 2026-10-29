package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtLoginData
import restarhalf.stellar.schedule.data.remote.JwxtLoginResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class JwxtAuthWorkflowPortImplTest {

    private val gateway = mock<JwxtGateway>(MockMode.autofill)
    private val store = JwxtAuthStore(MapSettings())
    private val impl = JwxtAuthWorkflowPortImpl(gateway, store)

    private fun stubLogin() {
        store.setToken("token")
        everySuspend { gateway.login(any(), any(), any(), any(), any()) } returns JwxtLoginResponse(
            code = 1,
            data = JwxtLoginData(userNo = "2023001"),
        )
    }

    @Test
    fun `显式登录开启新会话代次`() = runTest {
        stubLogin()
        val before = store.getSessionGeneration()

        impl.login("2023001", "password")

        assertEquals(before + 1, store.getSessionGeneration())
    }

    @Test
    fun `静默刷新保留会话代次`() = runTest {
        stubLogin()
        store.setCredentials("2023001", "password")
        val before = store.getSessionGeneration()

        impl.refreshSession()

        assertEquals(before, store.getSessionGeneration())
    }
}
