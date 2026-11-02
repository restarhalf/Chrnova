package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExactAlarmPermissionStoreTest {

    private val store = ExactAlarmPermissionStore(MapSettings())

    @Test
    fun askedDefaultsFalseAndStaysTrue() {
        assertFalse(store.hasAsked())
        store.markAsked()
        assertTrue(store.hasAsked())
        store.markAsked()
        assertTrue(store.hasAsked())
    }

    @Test
    fun allowedStartsUnknownAndRoundTrips() {
        assertNull(store.lastKnownAllowed())
        store.setLastKnownAllowed(false)
        assertEquals(false, store.lastKnownAllowed())
        store.setLastKnownAllowed(true)
        assertEquals(true, store.lastKnownAllowed())
    }
}
