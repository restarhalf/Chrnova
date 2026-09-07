package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwxtAuthPortImplTest {

    private val settings = MapSettings()
    private val store = JwxtAuthStore(settings)
    private val impl = JwxtAuthPortImpl(store)

    @Test
    fun tokenDefaultEmptyThenObserved() = runTest {
        assertEquals("", impl.observeToken().first())
        store.setToken("eyJhbGciOi")
        assertEquals("eyJhbGciOi", impl.observeToken().first())
    }

    @Test
    fun profileCombinesAllFields() = runTest {
        settings[JwxtAuthStore.KEY_NAME] = "张三"
        settings[JwxtAuthStore.KEY_USER_NO] = "2024000000"
        settings[JwxtAuthStore.KEY_CLS_NAME] = "计科2401"
        settings[JwxtAuthStore.KEY_ACADEMY_NAME] = "计算机学院"
        val profile = impl.observeProfile().first()
        assertEquals("张三", profile.name)
        assertEquals("2024000000", profile.userNo)
        assertEquals("计科2401", profile.clsName)
        assertEquals("计算机学院", profile.academyName)
    }

    @Test
    fun profileDefaultsEmpty() = runTest {
        val profile = impl.observeProfile().first()
        assertEquals("", profile.name)
        assertEquals("", profile.userNo)
    }

    @Test
    fun setCredentialsStoresSavedCredentials() {
        // setCredentials 写入的是"自动登录凭据"（saved_user_no/saved_password），
        // 不改写当前档案学号（KEY_USER_NO 由登录成功后的 setProfile 写入）
        impl.setCredentials("2024999999", "secret")
        assertEquals("2024999999" to "secret", store.getCredentials())
        assertNull(store.getUserNo())
    }

    @Test
    fun clearRemovesEverything() = runTest {
        store.setToken("tok")
        settings[JwxtAuthStore.KEY_USER_NO] = "2024000000"
        impl.clear()
        assertEquals("", impl.observeToken().first())
        assertNull(store.getUserNo())
    }
}
