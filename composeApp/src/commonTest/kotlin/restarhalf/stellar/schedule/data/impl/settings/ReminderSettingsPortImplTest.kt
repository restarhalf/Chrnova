package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderSettingsPortImplTest {

    private val impl = ReminderSettingsPortImpl(MapSettings())

    @Test
    fun reminderDefaultsFalseAndRoundTrip() = runTest {
        assertFalse(impl.observeCourseReminderEnabled().first())
        impl.setCourseReminderEnabled(true)
        assertTrue(impl.observeCourseReminderEnabled().first())

        assertFalse(impl.observeExamReminderEnabled().first())
        impl.setExamReminderEnabled(true)
        assertTrue(impl.observeExamReminderEnabled().first())
    }

    @Test
    fun exactAlarmAskedDefaultsFalseAndStaysTrue() {
        assertFalse(impl.hasAskedExactAlarmPermission())
        impl.markExactAlarmPermissionAsked()
        assertTrue(impl.hasAskedExactAlarmPermission())
        impl.markExactAlarmPermissionAsked()
        assertTrue(impl.hasAskedExactAlarmPermission())
    }

    @Test
    fun exactAlarmAllowedStartsUnknownAndRoundTrips() {
        assertNull(impl.getExactAlarmAllowed())
        impl.setExactAlarmAllowed(false)
        assertEquals(false, impl.getExactAlarmAllowed())
        impl.setExactAlarmAllowed(true)
        assertEquals(true, impl.getExactAlarmAllowed())
    }
}
