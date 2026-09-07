package restarhalf.stellar.schedule.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildHomeGreetingUseCaseTest {

    private val useCase = BuildHomeGreetingUseCase()

    @Test
    fun `少课时返回专属文案加通用文案`() {
        val list = useCase.candidates(courseCount = 0, hasFirstClass = false, dayOfWeekCount = 1)
        assertTrue(list.contains("你别上了，让我上"))
        assertTrue(list.contains("这才是我想要的大学生活"))
        assertEquals(4 + 15, list.size)
    }

    @Test
    fun `一节课也视为少课时`() {
        val list = useCase.candidates(courseCount = 1, hasFirstClass = false, dayOfWeekCount = 1)
        assertTrue(list.contains("翘课？不，我根本没课"))
    }

    @Test
    fun `中等课时不返回课时专属文案`() {
        val list = useCase.candidates(courseCount = 2, hasFirstClass = false, dayOfWeekCount = 1)
        assertFalse(list.contains("你别上了，让我上"))
        assertFalse(list.contains("又是课程满满的一天"))
        assertEquals(15, list.size)
    }

    @Test
    fun `三节课及以上返回满课文案`() {
        val list = useCase.candidates(courseCount = 3, hasFirstClass = false, dayOfWeekCount = 1)
        assertTrue(list.contains("又是课程满满的一天"))
        assertTrue(list.contains("大学五彩缤纷的是课表吗"))
        assertEquals(2 + 15, list.size)
    }

    @Test
    fun `有早八时追加早八文案`() {
        val list = useCase.candidates(courseCount = 2, hasFirstClass = true, dayOfWeekCount = 1)
        assertTrue(list.contains("早八如潮水般袭来"))
        assertTrue(list.contains("早八人，早八魂"))
        assertTrue(list.contains("已经没什么好害怕的了"))
    }

    @Test
    fun `星期四追加疯狂星期四文案`() {
        val list = useCase.candidates(courseCount = 2, hasFirstClass = false, dayOfWeekCount = 4)
        assertTrue(list.contains("疯狂星期四v我50"))
    }

    @Test
    fun `非星期四不含疯狂星期四`() {
        val list = useCase.candidates(courseCount = 2, hasFirstClass = false, dayOfWeekCount = 3)
        assertFalse(list.contains("疯狂星期四v我50"))
    }

    @Test
    fun `invoke 返回值来自候选池`() {
        val candidates = useCase.candidates(courseCount = 2, hasFirstClass = true, dayOfWeekCount = 4)
        repeat(20) {
            val greeting = useCase(courseCount = 2, hasFirstClass = true, dayOfWeekCount = 4)
            assertTrue(candidates.contains(greeting), "随机问候语应来自候选池: $greeting")
        }
    }
}
