package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwxtExaminationResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val emptySnapshot = """
        {"Msg":"success","code":"1","data":[]}
    """.trimIndent()

    private val nonEmptySnapshot = """
        {"Msg":"success","code":"1","data":[
          {"courseName":"高等数学","ksbz":"","courseNumber":"M1101","examinationPlace":"一教101","zwh":"05","time":"2026-01-12 09:00~11:00"},
          {"courseName":"大学物理","ksbz":"笔试","courseNumber":"P2001","examinationPlace":"二教203","zwh":"12","time":"2026-01-14 14:00~16:00"}
        ]}
    """.trimIndent()

    @Test
    fun `空考试安排成功形态`() {
        val resp = json.decodeFromString(JwxtExaminationResponse.serializer(), emptySnapshot)

        assertTrue(resp.isSuccess())
        assertEquals("success", resp.messageOrEmpty())
        assertEquals(0, resp.data.size)
    }

    @Test
    fun `考试项全字段断言`() {
        val resp = json.decodeFromString(JwxtExaminationResponse.serializer(), nonEmptySnapshot)

        assertTrue(resp.isSuccess())
        assertEquals(2, resp.data.size)

        val first = resp.data.first()
        assertEquals("高等数学", first.courseName)
        assertEquals("M1101", first.courseNumber)
        assertEquals("一教101", first.examinationPlace)
        assertEquals("05", first.zwh)
        assertEquals("2026-01-12 09:00~11:00", first.time)
        assertEquals("", first.ksbz)

        val second = resp.data[1]
        assertEquals("笔试", second.ksbz)
        assertEquals("二教203", second.examinationPlace)
        assertEquals("12", second.zwh)
    }
}
