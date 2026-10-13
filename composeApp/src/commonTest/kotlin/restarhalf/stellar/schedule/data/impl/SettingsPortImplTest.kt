package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.SettingsPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsPortImplTest {

    private val impl = SettingsPortImpl(MapSettings())



    @Test
    fun showNonCurrentWeekDefaultTrueAndRoundTrip() = runTest {
        assertTrue(impl.observeShowNonCurrentWeek().first())
        impl.setShowNonCurrentWeek(false)
        assertFalse(impl.observeShowNonCurrentWeek().first())
    }

    @Test
    fun reminderDefaultsFalseAndRoundTrip() = runTest {
        assertFalse(impl.observeCourseReminderEnabled().first())
        impl.setCourseReminderEnabled(true)
        assertTrue(impl.observeCourseReminderEnabled().first())

        assertFalse(impl.observeExamReminderEnabled().first())
        impl.setExamReminderEnabled(true)
        assertTrue(impl.observeExamReminderEnabled().first())

        assertFalse(impl.observeLogEnabled().first())
        impl.setLogEnabled(true)
        assertTrue(impl.observeLogEnabled().first())
    }



    @Test
    fun rowHeightClampedToRange() = runTest {
        impl.setScheduleRowHeight(1)
        assertEquals(SettingsPort.MIN_ROW_HEIGHT_DP, impl.observeScheduleRowHeight().first())
        impl.setScheduleRowHeight(999)
        assertEquals(SettingsPort.MAX_ROW_HEIGHT_DP, impl.observeScheduleRowHeight().first())
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
    fun floatingBarClampedTo012() = runTest {
        impl.setFloatingBar(9)
        assertEquals(2, impl.observeFloatingBar().first())
        impl.setFloatingBar(1)
        assertEquals(1, impl.observeFloatingBar().first())
    }



    @Test
    fun selectedTermRoundTrip() = runTest {
        assertEquals("", impl.observeSelectedTerm().first())
        impl.setSelectedTerm("2025-2026-1")
        assertEquals("2025-2026-1", impl.observeSelectedTerm().first())
    }

    @Test
    fun activeScheduleTermFallsBackToCurrentTermId() = runTest {
        impl.setCurrentTermId("2026-2027-1")

        impl.setActiveScheduleTerm("")
        assertEquals("2026-2027-1", impl.observeActiveScheduleTerm().first())

        impl.setActiveScheduleTerm("2025-2026-1")
        assertEquals("2025-2026-1", impl.observeActiveScheduleTerm().first())
    }

    @Test
    fun currentTermIdRoundTrip() = runTest {
        impl.setCurrentTermId("2025-2026-2")
        assertEquals("2025-2026-2", impl.observeCurrentTermId().first())
    }



    @Test
    fun shouldAutoSyncTrueWhenCooldownPassedAndMarks() = runTest {
        val now = 1_000_000_000L
        assertTrue(impl.shouldAutoSyncAndMark(now))

        assertFalse(impl.shouldAutoSyncAndMark(now + 1L))
        assertTrue(impl.shouldAutoSyncAndMark(now + 24L * 60 * 60 * 1000))
    }



    @Test
    fun deviceIdGeneratedOnceAndStable() {
        val first = impl.getDeviceId()
        assertEquals(32, first.length)
        assertEquals(first, impl.getDeviceId())

        val other = SettingsPortImpl(MapSettings()).getDeviceId()
        assertNotEquals(first, other)
    }



    @Test
    fun starVerifiedRoundTrip() {
        assertFalse(impl.getStarVerified())
        impl.setStarVerified(true)
        assertTrue(impl.getStarVerified())
    }



    @Test
    fun cachedSemesterIdsRoundTrip() = runTest {
        assertEquals(emptyList(), impl.observeCachedSemesterIds().first())
        val ids = listOf("2025-2026-1", "2025-2026-2")
        impl.setCachedSemesterIds(ids)
        assertEquals(ids, impl.observeCachedSemesterIds().first())
    }

    @Test
    fun cachedSemesterIdsBadJsonYieldsEmpty() = runTest {
        val impl = SettingsPortImpl(
            MapSettings(SettingsKeys.CACHED_SEMESTER_IDS to "{not json"),
        )
        assertEquals(emptyList(), impl.observeCachedSemesterIds().first())
    }



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
