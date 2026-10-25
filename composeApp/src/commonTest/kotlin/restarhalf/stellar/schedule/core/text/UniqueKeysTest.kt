package restarhalf.stellar.schedule.core.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UniqueKeysTest {

    @Test
    fun `首次出现保持原始 key`() {
        assertEquals("g1", uniqueKey("g1", 1))
    }

    @Test
    fun `后续出现追加序号`() {
        assertEquals("g1\u00002", uniqueKey("g1", 2))
    }

    @Test
    fun `重复 key 逐项生成不同结果`() {
        val keys = listOf("g1", "g1", "g1", "g2").uniqueKeys { it }

        assertEquals(4, keys.size)
        assertEquals(4, keys.toSet().size)
        assertEquals("g1", keys[0])
        assertEquals("g2", keys[3])
    }

    @Test
    fun `空列表返回空`() {
        assertTrue(emptyList<String>().uniqueKeys { it }.isEmpty())
    }

    @Test
    fun `业务 id 自带分隔符时不会与后缀撞车`() {
        // 若用 '#' 作分隔符，真实 id "g1#2" 会和 "g1" 的第 2 次出现撞车
        val keys = listOf("g1", "g1", "g1\u00002").uniqueKeys { it }

        assertEquals(3, keys.toSet().size)
        assertNotEquals(keys[1], keys[2])
    }
}
