package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JwxtCurriculumResponse 解析测试。
 *
 * fixture 为 2026-09-07 教务真实响应快照（GET njwhd/student/curriculum,
 * xnxq01id=2026-2027-1, week=1）：选课轮次尚未产生课表格子，item 为空，
 * 属于真实边界样本；教师/课程名为公开教学安排信息，无个人敏感数据。
 */
class JwxtCurriculumResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** 真实快照：week=1，item 空 + nkbList 两门固定课 */
    private val realSnapshotWeek1 = """
        {"Msg":"当前周次未查询到课表！","code":"1","data":[{"date":[
        {"xqmc":"一","mxrq":"2026-08-24","zc":"all","xqid":1},
        {"xqmc":"二","mxrq":"2026-08-25","zc":"all","xqid":2},
        {"xqmc":"三","mxrq":"2026-08-26","zc":"all","xqid":3},
        {"xqmc":"四","mxrq":"2026-08-27","zc":"all","xqid":4},
        {"xqmc":"五","mxrq":"2026-08-28","zc":"all","xqid":5},
        {"xqmc":"六","mxrq":"2026-08-29","zc":"all","xqid":6},
        {"xqmc":"日","mxrq":"2026-08-30","zc":"all","xqid":7}],
        "item":[],"week":3,"weekday":"一"}],
        "nowWeek":"1",
        "jcdatalist":[{"XJMC":"01,02","DJMC":"第一大节"},{"XJMC":"03,04","DJMC":"第二大节"},
        {"XJMC":"05,06","DJMC":"第三大节"},{"XJMC":"07,08","DJMC":"第四大节"},
        {"XJMC":"09,10","DJMC":"第五大节"},{"XJMC":"11,12","DJMC":"第六大节"}],
        "nkbList":[{"kch":"Y0003-5","kcmc":"劳动教育与训练5","jgxm":"于琬婷","xqname":"金石滩校区","zc":"1-20","tzdlb":"2","sjzcbz":""},
        {"kch":"E1072-1","kcmc":"专业综合实训I(校企合作)","jgxm":"薛明亮","xqname":"金石滩校区北区","zc":"1-3","tzdlb":"2","sjzcbz":""}]}
    """.trimIndent().replace("\n", "")

    @Test
    fun `真实快照week1字段完整解析`() {
        val r = json.decodeFromString(JwxtCurriculumResponse.serializer(), realSnapshotWeek1)

        assertTrue(r.isSuccess())
        assertEquals("1", r.code)
        assertEquals("当前周次未查询到课表！", r.msg)
        assertEquals("1", r.nowWeek)

        // 单日块：7 天日期表 + 空 item + Int 型 week 强转为 String
        assertEquals(1, r.data.size)
        val block = r.data.first()
        assertEquals(7, block.date.size)
        assertEquals("一", block.date.first().xqmc)
        assertEquals("2026-08-24", block.date.first().mxrq)
        assertEquals(1, block.date.first().xqid)
        assertEquals("日", block.date.last().xqmc)
        assertTrue(block.item.isEmpty())
        assertEquals("3", block.week)
        assertEquals("一", block.weekday)

        // 节次元数据：6 个大节
        assertEquals(6, r.jcDataList.size)
        assertEquals("01,02", r.jcDataList.first().xjmc)
        assertEquals("第一大节", r.jcDataList.first().djmc)
        assertEquals("11,12", r.jcDataList.last().xjmc)

        // 内班课表：两门固定课
        assertEquals(2, r.nkbList.size)
        assertEquals("Y0003-5", r.nkbList[0].kch)
        assertEquals("劳动教育与训练5", r.nkbList[0].kcmc)
        assertEquals("1-20", r.nkbList[0].zc)
        assertEquals("E1072-1", r.nkbList[1].kch)
        assertEquals("专业综合实训I(校企合作)", r.nkbList[1].kcmc)
        assertEquals("1-3", r.nkbList[1].zc)
        assertEquals("2", r.nkbList[1].tzdlb)
    }

    @Test
    fun `week字段Int与String双类型兼容`() {
        val intWeek = """{"code":"1","data":[{"date":[],"item":[],"week":3,"weekday":"一"}]}"""
        val strWeek = """{"code":"1","data":[{"date":[],"item":[],"week":"3","weekday":"一"}]}"""
        assertEquals("3", json.decodeFromString(JwxtCurriculumResponse.serializer(), intWeek).data.first().week)
        assertEquals("3", json.decodeFromString(JwxtCurriculumResponse.serializer(), strWeek).data.first().week)
    }

    /**
     * 真实 item（2026-09-07 金石滩校区 week=4 快照节选，字段未改动）。
     * 覆盖：classWeekDetails 前后带逗号、maxClassTime 用大节名、khfs 考试/考查两种。
     */
    @Test
    fun `真实非空item课程项解析`() {
        val body = """
            {"Msg":"success~","code":"1","data":[{"date":[],"item":[
            {"classWeek":"1-5,8-12,15","teacherName":"赫婧如","xqName":"金石滩校区","weekNoteDetail":"201,202",
            "buttonCode":"0","xqNumber":"2","ktmc":"营销[241-244]班","classTime":"20102",
            "jx0408id":"C8651EF2946E446FA9A44624F9CF1741","kch":"P0013",
            "courseName":"习近平新时代中国特色社会主义思想概论","isRepeatCode":"0","zxs":22,"peopleNumber":98,
            "maxClassTime":"第一大节","khfs":"考试","startTime":"08:30","endTIme":"10:00",
            "location":"弘德楼A区-308","classWeekDetails":",1,2,3,4,5,8,9,10,11,12,15,","coursesNote":1},
            {"classWeek":"4-5,8-11","teacherName":"云健","xqName":"金石滩校区北区","weekNoteDetail":"205,206",
            "buttonCode":"0","xqNumber":"4","ktmc":"计算机[241-244]班","classTime":"20506",
            "jx0408id":"1C4CF2F69FF44C1BADA48783C787DA6B","kch":"E1070","courseName":"区块链技术及应用",
            "isRepeatCode":"0","zxs":12,"peopleNumber":70,"maxClassTime":"第三大节","khfs":"考查",
            "startTime":"13:30","endTIme":"15:00","location":"致新楼西区-330",
            "classWeekDetails":",4,5,8,9,10,11,","coursesNote":1}],
            "week":4,"weekday":"四"}]}
        """.trimIndent().replace("\n", "")
        val r = json.decodeFromString(JwxtCurriculumResponse.serializer(), body)

        assertEquals("success~", r.msg)
        assertTrue(r.isSuccess())
        assertEquals("4", r.data.first().week)
        val items = r.data.first().item
        assertEquals(2, items.size)

        assertEquals("P0013", items[0].kch)
        assertEquals("习近平新时代中国特色社会主义思想概论", items[0].courseName)
        assertEquals("赫婧如", items[0].teacherName)
        assertEquals("弘德楼A区-308", items[0].location)
        assertEquals(",1,2,3,4,5,8,9,10,11,12,15,", items[0].classWeekDetails)
        assertEquals("第一大节", items[0].maxClassTime)
        assertEquals("20102", items[0].classTime)
        assertEquals(22, items[0].zxs)
        assertEquals(98, items[0].peopleNumber)
        assertEquals(1, items[0].coursesNote)

        assertEquals("E1070", items[1].kch)
        assertEquals("区块链技术及应用", items[1].courseName)
        assertEquals("考查", items[1].khfs)
        assertEquals("金石滩校区北区", items[1].xqName)
        assertEquals(",4,5,8,9,10,11,", items[1].classWeekDetails)
    }
}
