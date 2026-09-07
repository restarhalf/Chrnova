package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * wxgetXklc（选课轮次列表）真实响应快照解析测试。
 *
 * 快照来自 2026-09-07 教务系统真实响应（仅含轮次名称/时间等公开教学信息，无个人数据），
 * 覆盖 6 个选课轮次、两个学期。关键回归点：
 * - 响应包装 [JwxtSelectionResponse] 的 data 是 [kotlinx.serialization.json.JsonElement]（多端点复用），
 *   轮次数组需二次反序列化（与 CourseSelectionUseCase.parseList 相同路径）。
 * - 尾部 runTime 字段必须被 ignoreUnknownKeys 兜住。
 * - creditcontrol 在真实数据中为 null（可空字段）。
 */
class JwxtSelectionResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** 真实快照：wxgetXklc?isnew=1 完整响应（2026-09-07） */
    private val snapshot = """
        {"errorCode":"success","errorMessage":"success","errorMessageParam":[],"data":[{"xkjzsj":"2026-01-07 00:00","preselectedcourses":"否","drawlots":"未启用","creditcontrol":null,"xkkssj":"2026-01-06 00:00","rotationid":"864BBE67300A48BDBF71BF9060F38AE3","courseselectiontime":"2026-01-06 00:00~2026-01-07 00:00","dailycourseselection":"不控制","conflictselection":"是","semesterid":"2025-2026-2","courseselectioncontrol":"可选可退","rotationname":"压力测试选课","populationcontrol":"不控制","xqmc":"2025-2026-2","xkbs":"3"},{"xkjzsj":"2026-02-06 16:00","preselectedcourses":"是","drawlots":"未启用","creditcontrol":null,"xkkssj":"2026-02-05 09:00","rotationid":"1BDDAE00C3EC45C2AC100FE57A3EB48C","courseselectiontime":"2026-02-05 09:00~2026-02-06 16:00","dailycourseselection":"不控制","conflictselection":"否","semesterid":"2025-2026-2","courseselectioncontrol":"可选可退","rotationname":"2026春季学期24、25级公共和专业课选课","populationcontrol":"控制","xqmc":"2025-2026-2","xkbs":"3"},{"xkjzsj":"2026-08-28 12:00","preselectedcourses":"是","drawlots":"未启用","creditcontrol":null,"xkkssj":"2026-08-23 09:00","rotationid":"D4A1C0BE57CB4A6A9A692A3D5EC9BD95","courseselectiontime":"2026-08-23 09:00~2026-08-28 12:00","dailycourseselection":"00:00~23:40","conflictselection":"否","semesterid":"2026-2027-1","courseselectioncontrol":"可选可退","rotationname":"2026-2027学年秋季学期专业课、公共基础课补退选","populationcontrol":"控制","xqmc":"2026-2027-1","xkbs":"3"},{"xkjzsj":"2026-03-06 12:00","preselectedcourses":"是","drawlots":"未启用","creditcontrol":null,"xkkssj":"2026-02-27 09:00","rotationid":"A499256F017042128F12D16873C3128C","courseselectiontime":"2026-02-27 09:00~2026-03-06 12:00","dailycourseselection":"不控制","conflictselection":"否","semesterid":"2025-2026-2","courseselectioncontrol":"可选可退","rotationname":"2026春季学期补退选、微专业选课","populationcontrol":"控制","xqmc":"2025-2026-2","xkbs":"3"},{"xkjzsj":"2026-08-28 12:00","preselectedcourses":"是","drawlots":"未启用","creditcontrol":null,"xkkssj":"2026-08-23 02:00","rotationid":"00CAC6F10EAB4DE68941D2326511E79D","courseselectiontime":"2026-08-23 02:00~2026-08-28 12:00","dailycourseselection":"00:00~23:40","conflictselection":"是","semesterid":"2026-2027-1","courseselectioncontrol":"可选可退","rotationname":"2026-2027学年秋季学期微专业补退选","populationcontrol":"控制","xqmc":"2026-2027-1","xkbs":"3"},{"xkjzsj":"2026-02-11 16:00","preselectedcourses":"是","drawlots":"未启用","creditcontrol":null,"xkkssj":"2026-02-10 09:00","rotationid":"5AF69ECF0766429A9357787108D40E3B","courseselectiontime":"2026-02-10 09:00~2026-02-11 16:00","dailycourseselection":"不控制","conflictselection":"否","semesterid":"2025-2026-2","courseselectioncontrol":"可选可退","rotationname":"2026春季学期线上通识课选课","populationcontrol":"控制","xqmc":"2025-2026-2","xkbs":"3"}],"runTime":""}
    """.trimIndent()

    @Test
    fun `响应包装解析与isSuccess`() {
        val resp = json.decodeFromString(JwxtSelectionResponse.serializer(), snapshot)

        assertTrue(resp.isSuccess())
        assertEquals("success", resp.errorCode)
        assertEquals("success", resp.errorMessage)
        assertEquals(emptyList(), resp.errorMessageParam)
        assertEquals("", resp.runTime)
        // data 是数组元素
        assertEquals(6, resp.data?.jsonArray?.size)
    }

    @Test
    fun `轮次数组二次反序列化全字段断言`() {
        val resp = json.decodeFromString(JwxtSelectionResponse.serializer(), snapshot)
        // 与 CourseSelectionUseCase.parseList 相同的二次解析路径
        val rotations = json.decodeFromString(
            ListSerializer(JwxtSelectionRotation.serializer()),
            resp.data.toString(),
        )

        assertEquals(6, rotations.size)
        assertEquals(2, rotations.count { it.semesterId == "2026-2027-1" })

        // 当前学期补退选轮次逐字段校验
        val r = rotations.first { it.rotationId == "D4A1C0BE57CB4A6A9A692A3D5EC9BD95" }
        assertEquals("2026-2027学年秋季学期专业课、公共基础课补退选", r.rotationName)
        assertEquals("2026-2027-1", r.semesterId)
        assertEquals("2026-2027-1", r.xqmc)
        assertEquals("2026-08-23 09:00", r.startTime)
        assertEquals("2026-08-28 12:00", r.endTime)
        assertEquals("2026-08-23 09:00~2026-08-28 12:00", r.courseselectiontime)
        assertEquals("可选可退", r.courseselectioncontrol)
        assertEquals("00:00~23:40", r.dailycourseselection)
        assertEquals("是", r.preselectedcourses)
        assertEquals("未启用", r.drawlots)
        assertEquals("控制", r.populationcontrol)
        assertEquals("否", r.conflictselection)
        assertNull(r.creditcontrol)
        assertEquals("3", r.xkbs)
    }

    @Test
    fun `默认值兜底_缺字段与未知字段`() {
        // 未知字段 runTime 之外再塞入未知键 + 轮次缺全部可选字段，均不得抛异常
        val body = """
            {"errorCode":"success","errorMessage":"success","data":[
              {"rotationid":"TEST123"},
              {"rotationid":"T2","extraUnknown":"whatever"}
            ],"runTime":"12ms"}
        """.trimIndent()
        val resp = json.decodeFromString(JwxtSelectionResponse.serializer(), body)
        val rotations = json.decodeFromString(
            ListSerializer(JwxtSelectionRotation.serializer()),
            resp.data.toString(),
        )

        assertEquals("12ms", resp.runTime)
        assertEquals(2, rotations.size)
        assertEquals("TEST123", rotations[0].rotationId)
        assertEquals("", rotations[0].rotationName) // 默认值兜底
        assertEquals("T2", rotations[1].rotationId)
    }

    /**
     * 真实错误形态（2026-09-07 链路探测抓包）：
     * - wxinitXscache 选课窗口关闭
     * - wxgetKcList jsxsd 会话失效
     * - wxgetYxkcList 会话失效时的系统异常
     * 客户端靠 isSuccess()/isFail() + resolvedMessage() 统一转化为异常文案。
     */
    @Test
    fun `错误响应形态与文案解析`() {
        fun failBody(msg: String, params: String = "[]"): String =
            """{"errorCode":"fail","errorMessage":"$msg","errorMessageParam":$params,"data":"","runTime":""}"""

        // 1. 窗口关闭（wxinitXscache）
        val closed = json.decodeFromString(
            JwxtSelectionResponse.serializer(),
            failBody("当前未开放选课，具体请查看学校选课通知！！"),
        )
        assertTrue(closed.isFail())
        assertTrue(!closed.isSuccess())
        assertEquals("当前未开放选课，具体请查看学校选课通知！！", closed.resolvedMessage())

        // 2. 会话失效（wxgetKcList）
        val staleSession = json.decodeFromString(
            JwxtSelectionResponse.serializer(),
            failBody("当前账号已在别处登录，请重新登录进入选课！"),
        )
        assertEquals("当前账号已在别处登录，请重新登录进入选课！", staleSession.resolvedMessage())

        // 3. 系统异常（wxgetYxkcList）
        val sysError = json.decodeFromString(
            JwxtSelectionResponse.serializer(),
            failBody("系统异常，请联系管理员！！"),
        )
        assertEquals("系统异常，请联系管理员！！", sysError.resolvedMessage())

        // 4. 模板参数替换：errorMessageParam 逐个替换 [index] 占位符
        val withParams = json.decodeFromString(
            JwxtSelectionResponse.serializer(),
            failBody("还有[0]需要选", """["讲课学时"]"""),
        )
        assertEquals("还有讲课学时需要选", withParams.resolvedMessage())
        // data 为空串时服务端给的是字符串字面量，解码为 JsonPrimitive 不抛异常
        assertTrue(withParams.data is kotlinx.serialization.json.JsonPrimitive)
    }
}
