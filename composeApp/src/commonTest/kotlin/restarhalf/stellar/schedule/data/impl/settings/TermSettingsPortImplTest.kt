package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TermSettingsPortImplTest {

    private val impl = TermSettingsPortImpl(MapSettings())

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
    fun cachedSemesterIdsRoundTrip() = runTest {
        assertEquals(emptyList(), impl.observeCachedSemesterIds().first())
        val ids = listOf("2025-2026-1", "2025-2026-2")
        impl.setCachedSemesterIds(ids)
        assertEquals(ids, impl.observeCachedSemesterIds().first())
    }

    @Test
    fun cachedSemesterIdsBadJsonYieldsEmpty() = runTest {
        val impl = TermSettingsPortImpl(
            MapSettings(SettingsKeys.CACHED_SEMESTER_IDS to "{not json"),
        )
        assertEquals(emptyList(), impl.observeCachedSemesterIds().first())
    }
}
