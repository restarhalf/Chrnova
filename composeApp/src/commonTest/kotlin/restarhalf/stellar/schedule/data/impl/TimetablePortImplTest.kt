package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.data.local.TimetableSettings
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.policy.getCampusTimetable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TimetablePortImplTest {

    private val prefs = TimetableSettings(MapSettings())
    private val impl = TimetablePortImpl(prefs)

    @Test
    fun campusDefaultDevelopmentAndRoundTrip() = runTest {
        assertEquals(Campus.Development, impl.getCampus())
        assertEquals(Campus.Development, impl.observeCampus().first())
        impl.setCampus(Campus.Jinshitan)
        assertEquals(Campus.Jinshitan, impl.getCampus())
        assertEquals(Campus.Jinshitan, impl.observeCampus().first())
        impl.setCampus(Campus.Development)
        assertEquals(Campus.Development, impl.observeCampus().first())
    }

    @Test
    fun campusTimetableDelegatesToDomainPolicy() {
        for (campus in Campus.entries) {
            val expected = getCampusTimetable(campus)
            val actual = impl.getCampusTimetable(campus)
            assertEquals(expected.map { it.num }, actual.map { it.num })
            assertEquals(expected.map { it.start }, actual.map { it.start })
            assertEquals(expected.map { it.end }, actual.map { it.end })
            assertTrue(actual.isNotEmpty())
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun termStartDefaultsToTodaySoCurrentWeekIsOne() = runTest {
        val termStartMs = impl.getTermStartMs()
        val now = Clock.System.now().toEpochMilliseconds()

        assertTrue(termStartMs <= now)
        assertTrue(now - termStartMs < 25 * 60 * 60 * 1000L)
        assertEquals(termStartMs, impl.observeTermStartMs().first())

        val info =
            WeekCalculator.detect(
                totalWeeks = impl.getTotalWeeks(),
                termStartMs = termStartMs,
                nowMs = now,
            )
        assertFalse(info.isHoliday)
        assertEquals(1, info.week)
    }

    @Test
    fun termStartMsRoundTrip() = runTest {
        impl.setTermStartMs(1_770_000_000_000L)
        assertEquals(1_770_000_000_000L, impl.getTermStartMs())
        assertEquals(1_770_000_000_000L, impl.observeTermStartMs().first())
    }

    @Test
    fun totalWeeksRoundTrip() = runTest {
        impl.setTotalWeeks(18)
        assertEquals(18, impl.getTotalWeeks())
        assertEquals(18, impl.observeTotalWeeks().first())
    }
}
