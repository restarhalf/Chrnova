package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogSettingsPortImplTest {

    private val impl = LogSettingsPortImpl(MapSettings())

    @Test
    fun logEnabledDefaultsFalseAndRoundTrip() = runTest {
        assertFalse(impl.observeLogEnabled().first())
        impl.setLogEnabled(true)
        assertTrue(impl.observeLogEnabled().first())
    }
}
