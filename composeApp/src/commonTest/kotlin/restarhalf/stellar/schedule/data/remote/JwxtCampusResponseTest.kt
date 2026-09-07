package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JwxtCampusResponse（Get_sjkbms）解析测试。
 *
 * fixture 为 2026-09-07 教务真实响应快照：注意本端点 code 是数字（与
 * student/curriculum 的字符串 "1" 不同），isDefault 用 mrms 字段承载。
 */
class JwxtCampusResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val realSnapshot = """
        {"msg":"success","code":1,"data":[
        {"mrms":"1","kbjcmsid":"16FD8C2BE55E15F9E0630100007FF6B5","kbjcmsmc":"开发区校区"},
        {"mrms":"0","kbjcmsid":"D7B66244CCF84D07A01A69A5F36CA9DA","kbjcmsmc":"金石滩校区"}]}
    """.trimIndent().replace("\n", "")

    @Test
    fun `真实快照解析校区列表`() {
        val r = json.decodeFromString(JwxtCampusResponse.serializer(), realSnapshot)

        assertTrue(r.isSuccess())
        assertEquals(1, r.code)
        assertEquals(2, r.data.size)

        // 默认校区（mrms=1）在前
        assertTrue(r.data[0].isDefaultCampus())
        assertEquals("开发区校区", r.data[0].kbjcmsmc)
        assertEquals("16FD8C2BE55E15F9E0630100007FF6B5", r.data[0].kbjcmsid)

        assertFalse(r.data[1].isDefaultCampus())
        assertEquals("金石滩校区", r.data[1].kbjcmsmc)
        assertEquals("D7B66244CCF84D07A01A69A5F36CA9DA", r.data[1].kbjcmsid)
    }
}
