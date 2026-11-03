package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtLoginData
import restarhalf.stellar.schedule.data.remote.JwxtLoginResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun `刷新期间退出登录不会写回旧会话`() = runTest {
        stubLogin()
        store.setCredentials("2023001", "password")
        everySuspend { gateway.login(any(), any(), any(), any(), any()) } calls {
            store.clear()
            JwxtLoginResponse(code = 1, data = JwxtLoginData(userNo = "2023001"))
        }

        impl.refreshSession()

        assertNull(store.getToken())
        assertNull(store.getCredentials())
    }

    @Test
    fun `并发刷新只登录一次`() = runTest {
        stubLogin()
        store.setCredentials("2023001", "password")
        val loginStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var loginCount = 0
        everySuspend { gateway.login(any(), any(), any(), any(), any()) } calls {
            loginCount += 1
            loginStarted.complete(Unit)
            release.await()
            store.setToken("refreshed-token")
            JwxtLoginResponse(code = 1, data = JwxtLoginData(userNo = "2023001"))
        }

        val first = launch { impl.refreshSession() }
        loginStarted.await()
        val second = launch { impl.refreshSession() }
        advanceUntilIdle()
        release.complete(Unit)
        first.join()
        second.join()

        assertEquals(1, loginCount)
        assertEquals("refreshed-token", store.getToken())
    }

    @Test
    fun `刷新失败时恢复旧Token`() = runTest {
        store.setToken("old-token")
        store.setCredentials("2023001", "password")
        everySuspend { gateway.login(any(), any(), any(), any(), any()) } throws
            IllegalStateException("网络错误")

        impl.refreshSession()

        assertEquals("old-token", store.getToken())
        assertEquals("2023001", store.getCredentials()?.first)
    }

    @Test
    fun `刷新遇到账号或密码错误时退出登录`() = runTest {
        store.setToken("old-token")
        store.setCredentials("2023001", "password")
        everySuspend { gateway.login(any(), any(), any(), any(), any()) } returns
            JwxtLoginResponse(code = 0, msg = "该帐号不存在或密码错误！")

        impl.refreshSession()

        assertNull(store.getToken())
        assertNull(store.getCredentials())
    }

    @Test
    fun `刷新遇到HTTP层返回的密码错误时退出登录`() = runTest {
        store.setToken("old-token")
        store.setCredentials("2023001", "password")
        everySuspend { gateway.login(any(), any(), any(), any(), any()) } throws
            IllegalStateException("该帐号不存在或密码错误！")

        impl.refreshSession()

        assertNull(store.getToken())
        assertNull(store.getCredentials())
    }

    @Test
    fun `刷新遇到非凭据失败时保留会话`() = runTest {
        store.setToken("old-token")
        store.setCredentials("2023001", "password")
        everySuspend { gateway.login(any(), any(), any(), any(), any()) } returns
            JwxtLoginResponse(code = 0, msg = "系统繁忙，请稍后重试")

        impl.refreshSession()

        assertEquals("old-token", store.getToken())
        assertEquals("2023001", store.getCredentials()?.first)
    }
}
