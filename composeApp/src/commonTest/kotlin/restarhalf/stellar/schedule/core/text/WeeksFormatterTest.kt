package restarhalf.stellar.schedule.core.text

import kotlin.test.Test
import kotlin.test.assertEquals

class WeeksFormatterTest {

    @Test
    fun emptyListReturnsEmptyString() {
        assertEquals("", WeeksFormatter.format(emptyList()))
    }

    @Test
    fun singleWeek() {
        assertEquals("5周", WeeksFormatter.format(listOf(5)))
    }

    @Test
    fun unsortedInputIsSorted() {
        assertEquals("1-3周、5周", WeeksFormatter.format(listOf(5, 1, 3, 2)))
    }

    @Test
    fun consecutiveWeeksMerged() {
        assertEquals("1-3周、5周", WeeksFormatter.format(listOf(1, 2, 3, 5)))
    }

    @Test
    fun oddWeekRunGetsOddTag() {
        assertEquals("1-9周(单周)", WeeksFormatter.format(listOf(1, 3, 5, 7, 9)))
    }

    @Test
    fun evenWeekRunGetsEvenTag() {
        assertEquals("2-10周(双周)", WeeksFormatter.format(listOf(10, 8, 6, 4, 2)))
    }

    @Test
    fun twoItemEvenRunHasNoTag() {
        // 步长2但不足3个，不加单双周标记
        assertEquals("2周、4周", WeeksFormatter.format(listOf(2, 4)))
    }

    @Test
    fun gapBreaksRun() {
        assertEquals("1-2周、4周", WeeksFormatter.format(listOf(1, 2, 4)))
    }

    @Test
    fun duplicatesCollapseIntoRun() {
        // 排序后 [2,2,4,4,6,6]：2→2 步长0，落到单周分支，之后 4→4 同理
        val result = WeeksFormatter.format(listOf(2, 2, 4, 4, 6, 6))
        assertEquals("2周、2周、4周、4周、6周、6周", result)
    }
}
