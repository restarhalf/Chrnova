package restarhalf.stellar.schedule.data.local

import kotlin.test.Test
import kotlin.test.assertEquals

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun listRoundTrip() {
        val weeks = listOf(1, 3, 5, 7, 9)
        assertEquals(weeks, converters.toList(converters.fromList(weeks)))
    }

    @Test
    fun emptyListRoundTrip() {
        assertEquals(emptyList(), converters.toList(converters.fromList(emptyList())))
    }

    @Test
    fun singleElementRoundTrip() {
        assertEquals(listOf(42), converters.toList(converters.fromList(listOf(42))))
    }

    @Test
    fun negativeAndLargeNumbers() {
        val values = listOf(-5, 0, 999_999)
        assertEquals(values, converters.toList(converters.fromList(values)))
    }

    @Test
    fun storedFormatIsJsonArray() {
        assertEquals("[1,2,3]", converters.fromList(listOf(1, 2, 3)))
    }
}
