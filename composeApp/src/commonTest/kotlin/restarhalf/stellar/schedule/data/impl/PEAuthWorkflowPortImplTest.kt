package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import dev.mokkery.everySuspend
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.remote.PEAuthStore
import restarhalf.stellar.schedule.domain.model.PELoginResponse
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.repository.PERepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PEAuthWorkflowPortImplTest {

    private val gateway = mock<PEGateway>(MockMode.autofill)
    private val repository = mock<PERepository>(MockMode.autofill)
    private val store = PEAuthStore(MapSettings())
    private val impl = PEAuthWorkflowPortImpl(gateway, store, repository)

    @Test
    fun `退出会清空会话凭据和本地体育数据`() = runTest {
        store.setToken("token")
        store.setUserId("2023001")
        store.setLastUsername("2023001")
        store.setCredentials("2023001", "password")

        impl.logout()

        assertNull(store.getToken())
        assertNull(store.getUserId())
        assertNull(store.getLastUsername())
        assertNull(store.getCredentials())
        verifySuspend(VerifyMode.exactly(1)) { repository.clearUser("2023001") }
    }

    @Test
    fun `退出后不会使用已保存凭据自动重新登录`() = runTest {
        store.setToken("token")
        store.setLastUsername("2023001")
        store.setCredentials("2023001", "password")

        impl.logout()

        val error = assertFailsWith<IllegalStateException> { impl.ensureLoggedIn() }
        assertEquals("请先登录", error.message)
        assertNull(store.getToken())
        assertNull(store.getCredentials())
        verifySuspend(VerifyMode.not) { gateway.login(any(), any()) }
    }

    @Test
    fun `退出登录时进行中的刷新不会恢复会话`() = runTest {
        store.setToken("old-token")
        store.setCredentials("2023001", "password")
        val loginStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        everySuspend { gateway.login(any(), any()) } calls {
            loginStarted.complete(Unit)
            release.await()
            store.setToken("new-token")
            store.setUserId("new-user")
            PELoginResponse(status = "PASS", token = "new-token", userId = "new-user")
        }

        val refreshJob = launch { impl.refreshSession() }
        loginStarted.await()
        val logoutJob = launch { impl.logout() }
        advanceUntilIdle()
        release.complete(Unit)
        refreshJob.join()
        logoutJob.join()

        assertNull(store.getToken())
        assertNull(store.getCredentials())
        assertNull(store.getLastUsername())
    }

    @Test
    fun `登录响应缺少token时清会话并失败`() = runTest {
        store.setToken("old-token")
        store.setUserId("old-user")
        store.setCredentials("2023001", "password")
        everySuspend { gateway.login(any(), any()) } returns
            PELoginResponse(status = "PASS", token = null, userId = "u1")

        val error = assertFailsWith<IllegalStateException> { impl.login("2023001", "password") }

        assertEquals("登录响应缺少 Token 或用户标识，请重新登录", error.message)
        assertNull(store.getToken())
        assertNull(store.getUserId())
    }

    @Test
    fun `登录响应缺少userId时清会话并失败`() = runTest {
        store.setToken("old-token")
        store.setUserId("old-user")
        store.setCredentials("2023001", "password")
        everySuspend { gateway.login(any(), any()) } returns
            PELoginResponse(status = "PASS", token = "new-token", userId = " ")

        val error = assertFailsWith<IllegalStateException> { impl.login("2023001", "password") }

        assertEquals("登录响应缺少 Token 或用户标识，请重新登录", error.message)
        assertNull(store.getToken())
        assertNull(store.getUserId())
    }

    @Test
    fun `登录响应完整时写入会话与凭据`() = runTest {
        everySuspend { gateway.login(any(), any()) } calls {
            store.setToken("new-token")
            store.setUserId("u1")
            PELoginResponse(status = "PASS", token = "new-token", userId = "u1")
        }

        impl.login("2023001", "password")

        assertEquals("new-token", store.getToken())
        assertEquals("u1", store.getUserId())
        assertEquals("2023001" to "password", store.getCredentials())
    }

    @Test
    fun `刷新失败时清理会话并保留保存的凭据`() = runTest {
        store.setToken("dead-token")
        store.setUserId("2023001")
        store.setProfile("小明", "2023001", "T1")
        store.setLastUsername("2023001")
        store.setCredentials("2023001", "password")
        everySuspend { gateway.login(any(), any()) } throws IllegalStateException("登录失败")

        impl.refreshSession()

        assertNull(store.getToken())
        assertNull(store.getUserId())
        assertNull(store.getStdNumber())
        assertEquals("2023001", store.getLastUsername())
        assertEquals("2023001" to "password", store.getCredentials())
    }

    @Test
    fun `没有保存凭据时刷新会清理会话且不尝试登录`() = runTest {
        store.setToken("dead-token")
        store.setUserId("2023001")

        impl.refreshSession()

        assertNull(store.getToken())
        assertNull(store.getUserId())
        verifySuspend(VerifyMode.not) { gateway.login(any(), any()) }
    }

    @Test
    fun `刷新失败后token流广播为未登录`() = runTest {
        val authPort = PEAuthPortImpl(store)
        store.setToken("dead-token")
        store.setUserId("2023001")
        store.setCredentials("2023001", "password")
        everySuspend { gateway.login(any(), any()) } throws IllegalStateException("登录失败")
        val tokens = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            authPort.observeToken().collect { tokens += it }
        }
        advanceUntilIdle()

        impl.refreshSession()
        advanceUntilIdle()
        job.cancel()

        assertEquals("dead-token", tokens.first())
        assertEquals("", tokens.last())
    }

    @Test
    fun `并发刷新只登录一次`() = runTest {
        store.setToken("old-token")
        store.setCredentials("2023001", "password")
        val loginStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var loginCount = 0
        everySuspend { gateway.login(any(), any()) } calls {
            loginCount += 1
            loginStarted.complete(Unit)
            release.await()
            store.setToken("refreshed-token")
            store.setUserId("u1")
            PELoginResponse(status = "PASS", token = "refreshed-token", userId = "u1")
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
}
