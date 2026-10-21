package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceSettingsPortImplTest {

    private val impl = DeviceSettingsPortImpl(MapSettings())

    @Test
    fun deviceIdGeneratedOnceAndStable() {
        val first = impl.getDeviceId()
        assertEquals(32, first.length)
        assertEquals(first, impl.getDeviceId())

        val other = DeviceSettingsPortImpl(MapSettings()).getDeviceId()
        assertNotEquals(first, other)
    }

    @Test
    fun starVerifiedRoundTrip() {
        assertFalse(impl.getStarVerified())
        impl.setStarVerified(true)
        assertTrue(impl.getStarVerified())
    }

    @Test
    fun confirmPrivacyDefaultsFalseAndRoundTrip() {
        assertFalse(impl.getConfirmPrivacy())
        impl.setConfirmPrivacy(true)
        assertTrue(impl.getConfirmPrivacy())
        impl.setConfirmPrivacy(false)
        assertFalse(impl.getConfirmPrivacy())
    }

    @Test
    fun dauLastPingDayNullThenRoundTrip() {
        assertNull(impl.getDauLastPingDay())
        impl.setDauLastPingDay("2026-10-21")
        assertEquals("2026-10-21", impl.getDauLastPingDay())
    }
}
