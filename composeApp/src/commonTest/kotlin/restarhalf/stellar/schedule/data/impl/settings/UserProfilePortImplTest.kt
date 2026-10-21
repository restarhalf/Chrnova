package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserProfilePortImplTest {

    private val impl = UserProfilePortImpl(MapSettings())

    @Test
    fun avatarUriNullAndBlankNormalized() {
        assertNull(impl.getUserAvatarUri())
        impl.setUserAvatarUri("content://gallery/1")
        assertEquals("content://gallery/1", impl.getUserAvatarUri())
        impl.setUserAvatarUri("   ")
        assertNull(impl.getUserAvatarUri())
        impl.setUserAvatarUri(null)
        assertNull(impl.getUserAvatarUri())
    }

    @Test
    fun nicknameNullAndBlankNormalized() {
        assertNull(impl.getUserNickname())
        impl.setUserNickname("小明")
        assertEquals("小明", impl.getUserNickname())
        impl.setUserNickname("")
        assertNull(impl.getUserNickname())
        impl.setUserNickname(null)
        assertNull(impl.getUserNickname())
    }
}
