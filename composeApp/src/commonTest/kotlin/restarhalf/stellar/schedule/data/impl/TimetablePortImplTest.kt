package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.local.Campus as DataCampus
import restarhalf.stellar.schedule.data.local.getCampusTimetable as getDataTimetable
import restarhalf.stellar.schedule.data.local.TimetableSettings
import restarhalf.stellar.schedule.data.mapper.toData
import restarhalf.stellar.schedule.domain.model.Campus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun campusTimetableDelegatesToLocalConfig() {
        for (campus in Campus.entries) {
            val expected = getDataTimetable(campus.toData())
            val actual = impl.getCampusTimetable(campus)
            assertEquals(expected.map { it.num }, actual.map { it.num })
            assertEquals(expected.map { it.start }, actual.map { it.start })
            assertEquals(expected.map { it.end }, actual.map { it.end })
            assertTrue(actual.isNotEmpty())
        }
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

    @Test
    fun campusEnumMirror() {
        // data 层与 domain 层 Campus 枚举一一对应
        assertEquals(Campus.entries.size, DataCampus.entries.size)
    }
}
