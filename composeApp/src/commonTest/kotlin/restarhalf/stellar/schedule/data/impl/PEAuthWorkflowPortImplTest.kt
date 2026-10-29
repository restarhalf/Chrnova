package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.remote.PEAuthStore
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
        verifySuspend(VerifyMode.exactly(1)) { repository.clearAll() }
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
}
