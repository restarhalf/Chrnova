package restarhalf.stellar.schedule.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolveCourseStatusUseCaseTest {

    private val useCase = ResolveCourseStatusUseCase()

    @Test
    fun `无课程返回null`() {
        assertNull(useCase(false, "08:00", "08:45", 480))
    }

    @Test
    fun `开始前返回未开始`() {
        assertEquals("未开始", useCase(true, "08:00", "08:45", 479))
    }

    @Test
    fun `开始时间点返回进行中`() {
        assertEquals("进行中", useCase(true, "08:00", "08:45", 480))
    }

    @Test
    fun `课程中间返回进行中`() {
        assertEquals("进行中", useCase(true, "08:00", "08:45", 500))
    }

    @Test
    fun `结束分钟点返回进行中`() {
        assertEquals("进行中", useCase(true, "08:00", "08:45", 525))
    }

    @Test
    fun `结束后返回已结束`() {
        assertEquals("已结束", useCase(true, "08:00", "08:45", 526))
    }

    @Test
    fun `开始时间非法返回null`() {
        assertNull(useCase(true, "abc", "08:45", 480))
    }

    @Test
    fun `结束时间非法返回null`() {
        assertNull(useCase(true, "08:00", "", 480))
    }
}
