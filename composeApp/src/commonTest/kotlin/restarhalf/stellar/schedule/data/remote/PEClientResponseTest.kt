package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.model.PEAppointmentListResponse
import restarhalf.stellar.schedule.domain.model.PEAuthProfileResponse
import restarhalf.stellar.schedule.domain.model.PEDetailResponse
import restarhalf.stellar.schedule.domain.model.PEFreeApplyListResponse
import restarhalf.stellar.schedule.domain.model.PELoginResponse
import restarhalf.stellar.schedule.domain.model.PEScoreListResponse
import restarhalf.stellar.schedule.domain.model.PESubjectHistoryResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PEClientResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val loginSnapshot = """
        {"school_date":"2024","gender":"m","user_name":"张三","classes":"计算机科学与技术244",
        "user_fun":{"action_funs":["/mobile/appointment/deleteRepealAppointment","/sysUser/mobile/findStudent"],
        "view_funs":[{"path":"/my","title":"个人中心","children":[]}]},
        "school_name":"大连民族大学","message":"登陆成功","token":"test-token:abcdef0123456789",
        "user_id":"test-user-id","is_passwd_upd":"1","std_number":"2024000000",
        "department":"计算机科学与工程学院","status":"PASS"}
    """.trimIndent().replace("\n", "")

    @Test
    fun `登录响应解析与未知字段兜底`() {
        val r = json.decodeFromString(PELoginResponse.serializer(), loginSnapshot)

        assertEquals("PASS", r.status)
        assertEquals("登陆成功", r.message)
        assertEquals("test-token:abcdef0123456789", r.token)
        assertEquals("test-user-id", r.userId)

    }

    private val planListSnapshot = """
        {"data_arr":[{"total":52.00,"is_free":0,"school_year":"2025","done":7,"nums":8},
        {"total":50.20,"is_free":0,"school_year":"2024","done":7,"nums":8}],
        "message":"获取信息成功","status":"PASS"}
    """.trimIndent().replace("\n", "")

    @Test
    fun `学年成绩列表解析`() {
        val r = json.decodeFromString(PEScoreListResponse.serializer(), planListSnapshot)

        assertEquals("PASS", r.status)
        assertEquals(2, r.dataArr.size)
        assertEquals("2025", r.dataArr[0].schoolYear)
        assertEquals(52.0, r.dataArr[0].total)
        assertEquals(7, r.dataArr[0].done)
        assertEquals(8, r.dataArr[0].nums)

        assertEquals(50.2, r.dataArr[1].total)
    }

    private val scoreDetailSnapshot = """
        {"data":{"total_grade":"不及格","total_score":52.00,"data_arr":[
        {"subject_id":"1f8cd97c401a4f608313e531f318e324","result":"5404","score":100,"unit":"ml",
        "sub_name":"肺活量","sub_ratio":"15","grade":"优秀","is_join":1,"icon":"","source_type":"0",
        "school_year":"2025","source_code":"fhl"},
        {"subject_id":"11e927dd64c5446a80b7ee0c71ec8a2f","result":"33.9","score":60,"unit":"kg/m²",
        "sub_name":"身高体重（BMI）","sub_ratio":"15","grade":"肥胖","is_join":1,"source_type":"0",
        "school_year":"2025","source_code":"bmi"},
        {"subject_id":"e8ec94f87250498ab0bcdce877616564","unit":"times","sub_name":"引体向上",
        "sub_ratio":"10","is_join":0,"icon":"","source_type":"0","school_year":"2025","source_code":"ytxs"}]},
        "message":"获取信息成功","status":"PASS"}
    """.trimIndent().replace("\n", "")

    @Test
    fun `成绩详情解析与未参加项目可空字段`() {
        val r = json.decodeFromString(PEDetailResponse.serializer(), scoreDetailSnapshot)

        assertEquals("PASS", r.status)
        assertNotNull(r.data)
        assertEquals(52.0, r.data.totalScore)
        assertEquals("不及格", r.data.totalGrade)
        assertEquals(3, r.data.dataArr.size)

        val fhl = r.data.dataArr.first { it.subName == "肺活量" }
        assertEquals("5404", fhl.result)
        assertEquals(100, fhl.score)
        assertEquals("ml", fhl.unit)
        assertEquals("15", fhl.subRatio)
        assertEquals("优秀", fhl.grade)
        assertEquals(1, fhl.isJoin)
        assertEquals("1f8cd97c401a4f608313e531f318e324", fhl.subjectId)

        val ytxs = r.data.dataArr.first { it.subName == "引体向上" }
        assertEquals(0, ytxs.isJoin)
        assertNull(ytxs.result)
        assertNull(ytxs.score)
        assertNull(ytxs.grade)
        assertEquals("times", ytxs.unit)
    }

    private val subjectHistorySnapshot = """
        {"data":{"data_list":[
        {"result":"5404","session_name":"2025-2026学年体能测试体测考核","score_time":"2026-06-07 15:15:59",
        "sourceScoreId":"8d139d7734a94c0db307432a367c3439","sub_name":"肺活量","score_status":"2"},
        {"result":"4371","session_name":"2025-2026学年体能测试体测考核","score_time":"2026-06-07 15:05:19",
        "sourceScoreId":"ef910bd4b72b48f38d6599d22f82e364","sub_name":"肺活量","score_status":"1"},
        {"result":"2608","session_name":"2025-2026学年体能测试体测考核","score_time":"2025-11-10 18:39:53",
        "sourceScoreId":"3effe169d3514903900315dccac714c7","sub_name":"肺活量","score_status":"1"}],
        "total_rows":6},"message":"获取信息成功","status":"PASS"}
    """.trimIndent().replace("\n", "")

    @Test
    fun `单项成绩历史解析`() {
        val r = json.decodeFromString(PESubjectHistoryResponse.serializer(), subjectHistorySnapshot)

        assertEquals("PASS", r.status)
        assertNotNull(r.data)
        assertEquals(6, r.data.totalRows)
        assertEquals(3, r.data.dataList.size)

        val best = r.data.dataList.first()
        assertEquals("5404", best.result)
        assertEquals("2025-2026学年体能测试体测考核", best.sessionName)
        assertEquals("2026-06-07 15:15:59", best.scoreTime)
        assertEquals("2", best.scoreStatus)
        assertEquals("肺活量", best.subName)
    }

    private val profileSnapshot = """
        {"data":{"studentTagCode":"","testCode":"id=2024000000&name=%E5%BC%A0%E4%B8%89","tagCode":"",
        "gender":"m","classes":"计算机科学与技术244","HRmax":"203","stuName":"张三","message":"获取信息成功",
        "department":"计算机科学与工程学院","schoolDate":"2024","stdNumber":"2024000000","status":"PASS"},
        "message":"获取信息成功","status":"PASS"}
    """.trimIndent().replace("\n", "")

    @Test
    fun `学生信息解析`() {
        val r = json.decodeFromString(PEAuthProfileResponse.serializer(), profileSnapshot)

        assertEquals("PASS", r.status)
        assertNotNull(r.data)
        assertEquals("张三", r.data.stuName)
        assertEquals("2024000000", r.data.stdNumber)
        assertEquals("id=2024000000&name=%E5%BC%A0%E4%B8%89", r.data.testCode)

    }
}
