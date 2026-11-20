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
        assertEquals(
            DisplaySettingsPort.DEFAULT_ROW_HEIGHT_DP,
            impl.observeScheduleRowHeight().first(),
        )
        impl.setScheduleRowHeight(1)
        assertEquals(DisplaySettingsPort.MIN_ROW_HEIGHT_DP, impl.observeScheduleRowHeight().first())
        impl.setScheduleRowHeight(999)
        assertEquals(DisplaySettingsPort.MAX_ROW_HEIGHT_DP, impl.observeScheduleRowHeight().first())
        impl.setScheduleRowHeight(88)
        assertEquals(88, impl.observeScheduleRowHeight().first())
    }

    @Test
    fun themeModeClampedToValidModes() = runTest {
        impl.setThemeMode(5)
        assertEquals(DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER, impl.observeThemeMode().first())
        impl.setThemeMode(-1)
        assertEquals(DisplaySettingsPort.THEME_MODE_SYSTEM, impl.observeThemeMode().first())
        impl.setThemeMode(1)
        assertEquals(DisplaySettingsPort.THEME_MODE_LIGHT, impl.observeThemeMode().first())
        impl.setThemeMode(3)
        assertEquals(DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER, impl.observeThemeMode().first())
    }


    @Test
    fun themeModeSyncGetterMatchesFlow() = runTest {
        assertEquals(0, impl.getThemeMode())
        impl.setThemeMode(2)
        assertEquals(2, impl.getThemeMode())
        assertEquals(2, impl.observeThemeMode().first())
    }
}
