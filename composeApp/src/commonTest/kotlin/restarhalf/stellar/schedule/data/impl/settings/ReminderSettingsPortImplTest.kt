package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderSettingsPortImplTest {

    private val impl = ReminderSettingsPortImpl(MapSettings())

    @Test
    fun reminderDefaultsFalseAndRoundTrip() = runTest {
        assertFalse(impl.observeCourseReminderEnabled().first())
        impl.setCourseReminderEnabled(true)
        assertTrue(impl.observeCourseReminderEnabled().first())

        // 考试提醒沿用课程提醒：没有独立开关，跟随 REMINDER_ENABLED
        assertTrue(impl.observeExamReminderEnabled().first())
        impl.setCourseReminderEnabled(false)
        assertFalse(impl.observeExamReminderEnabled().first())
    }
}
