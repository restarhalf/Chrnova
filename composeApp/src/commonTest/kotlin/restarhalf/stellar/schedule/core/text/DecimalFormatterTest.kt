package restarhalf.stellar.schedule.core.text

import kotlin.test.Test
import kotlin.test.assertEquals

class DecimalFormatterTest {

    @Test
    fun zeroDecimalsRoundsToInteger() {
        assertEquals("3", DecimalFormatter.format(2.6, 0))
        assertEquals("3", DecimalFormatter.format(2.5, 0))
        assertEquals("2", DecimalFormatter.format(2.4, 0))
    }

    @Test
    fun negativeDecimalsCoercedToZero() {
        assertEquals("3", DecimalFormatter.format(3.14, -2))
    }

    @Test
    fun twoDecimals() {
        assertEquals("123.46", DecimalFormatter.format(123.456, 2))
        assertEquals("123.45", DecimalFormatter.format(123.454, 2))
    }

    @Test
    fun exactHalfRoundsUp() {
        assertEquals("1.3", DecimalFormatter.format(1.25, 1))
    }

    @Test
    fun trailingZerosPadded() {
        assertEquals("1.50", DecimalFormatter.format(1.5, 2))
        assertEquals("1.05", DecimalFormatter.format(1.05, 2))
    }

    @Test
    fun negativeValueKeepsSign() {
        assertEquals("-3.14", DecimalFormatter.format(-3.14159, 2))
        assertEquals("-3", DecimalFormatter.format(-3.4, 0))
    }

    @Test
    fun zeroValue() {
        assertEquals("0.00", DecimalFormatter.format(0.0, 2))
    }

    @Test
    fun floatOverloadDelegates() {
        assertEquals("0.5", DecimalFormatter.format(0.5f, 1))
    }
}
