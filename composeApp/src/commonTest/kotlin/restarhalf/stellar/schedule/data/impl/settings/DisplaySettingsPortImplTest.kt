package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisplaySettingsPortImplTest {

    private val impl = DisplaySettingsPortImpl(MapSettings())

    @Test
    fun showNonCurrentWeekDefaultTrueAndRoundTrip() = runTest {
        assertTrue(impl.observeShowNonCurrentWeek().first())
        impl.setShowNonCurrentWeek(false)
        assertFalse(impl.observeShowNonCurrentWeek().first())
    }

    @Test
    fun rowHeightClampedToRange() = runTest {
        impl.setScheduleRowHeight(1)
        assertEquals(DisplaySettingsPort.MIN_ROW_HEIGHT_DP, impl.observeScheduleRowHeight().first())
        impl.setScheduleRowHeight(999)
        assertEquals(DisplaySettingsPort.MAX_ROW_HEIGHT_DP, impl.observeScheduleRowHeight().first())
        impl.setScheduleRowHeight(88)
        assertEquals(88, impl.observeScheduleRowHeight().first())
    }

    @Test
    fun themeModeClampedTo012() = runTest {
        impl.setThemeMode(5)
        assertEquals(2, impl.observeThemeMode().first())
        impl.setThemeMode(-1)
        assertEquals(0, impl.observeThemeMode().first())
        impl.setThemeMode(1)
        assertEquals(1, impl.observeThemeMode().first())
    }

    @Test
    fun themeModeSyncGetterMatchesFlow() = runTest {
        assertEquals(0, impl.getThemeMode())
        impl.setThemeMode(2)
        assertEquals(2, impl.getThemeMode())
        assertEquals(2, impl.observeThemeMode().first())
    }

    @Test
    fun floatingBarClampedTo012() = runTest {
        impl.setFloatingBar(9)
        assertEquals(2, impl.observeFloatingBar().first())
        impl.setFloatingBar(1)
        assertEquals(1, impl.observeFloatingBar().first())
    }

    @Test
    fun floatingBarSyncGetterMatchesFlow() = runTest {
        assertEquals(0, impl.getFloatingBar())
        impl.setFloatingBar(2)
        assertEquals(2, impl.getFloatingBar())
        assertEquals(2, impl.observeFloatingBar().first())
    }
}
