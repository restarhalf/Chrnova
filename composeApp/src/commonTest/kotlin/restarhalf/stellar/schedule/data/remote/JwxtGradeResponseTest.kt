package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * termGPA（学期成绩单）真实响应快照解析测试。
 *
 * 快照来自 2026-09-07 教务系统真实响应（semester=2025-2026-2），已脱敏：
 * 姓名→张三、学号→2024000000，课程数据原样保留。关键回归点：
 * - `fraction` 是 String 型分数（"80"），`jd`/`credit` 是 Double（3.0/3.5）——类型混搭易碎。
 * - 新学期响应 achievement/xqgpa 为空数组、pjcj 为空串（真实边界）。
 * - 学分小数（3.5）与绩点小数（2.4/4.1）必须无损解析。
 */
class JwxtGradeResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** 真实快照（脱敏、achievement 节选 5/12 门）：termGPA?semester=2025-2026-2&type=1 */
    private val snapshot = """
        {"Msg":"success","code":"1","data":[{"studentID":"2024000000","xqgpa":[],"inGrade":"2024","pjcj":"","name":"张三","yxzxf":"120","zxfjd":"394.3","pjxfjd":"3.32","achievement":[{"curriculumAttributes":"必修","cjdj":"","sfjg":"合格","examName":"考试","courseNature":"通识必修","kkxq":"2025-2026-2","kcbh":"D0001-4","fraction":"80","courseName":"大学英语4","bcxq":"","cjbs":"","examinationNature":"正常考试","jd":3,"credit":2,"cj0708id":"56F41E75456A9A9AE0630F061ED2C36E"},{"curriculumAttributes":"必修","cjdj":"","sfjg":"合格","examName":"考查","courseNature":"专业教育平台课程","kkxq":"2025-2026-2","kcbh":"E1007","fraction":"70","courseName":"计算机组成原理","bcxq":"","cjbs":"","examinationNature":"正常考试","jd":2,"credit":3.5,"cj0708id":"567D3078F1196547E0630F061ED25838"},{"curriculumAttributes":"任选","cjdj":"","sfjg":"合格","examName":"考查","courseNature":"专业选修","kkxq":"2025-2026-2","kcbh":"E1008a-2","fraction":"74","courseName":"工作室课题2","bcxq":"","cjbs":"","examinationNature":"正常考试","jd":2.4,"credit":2,"cj0708id":"5495985EF493C400E0630F061ED28ED3"},{"curriculumAttributes":"必修","cjdj":"","sfjg":"合格","examName":"考查","courseNature":"专业实践","kkxq":"2025-2026-2","kcbh":"E1033","fraction":"95","courseName":"数据库与信息管理课程设计","bcxq":"","cjbs":"","examinationNature":"正常考试","jd":4.5,"credit":3,"cj0708id":"56389959D7DCA62BE0630F061ED27D0B"},{"curriculumAttributes":"必修","cjdj":"","sfjg":"合格","examName":"考试","courseNature":"专业教育平台课程","kkxq":"2025-2026-2","kcbh":"E1049","fraction":"91","courseName":"数据库原理与应用","bcxq":"","cjbs":"","examinationNature":"正常考试","jd":4.1,"credit":3,"cj0708id":"53B62F21EC5ABE78E0630F061ED2F2EA"}]}]}
    """.trimIndent()

    /** 真实快照：新学期（2026-2027-1）尚无成绩的空形态 */
    private val emptySnapshot = """
        {"Msg":"success","code":"1","data":[{"studentID":"2024000000","xqgpa":[],"inGrade":"2024","pjcj":"","achievement":[],"name":"张三","yxzxf":"120","zxfjd":"394.3","pjxfjd":"3.32"}]}
    """.trimIndent()

    @Test
    fun `成绩单快照解析_汇总字段`() {
        val resp = json.decodeFromString(
            JwxtApiResponse.serializer(ListSerializer(JwxtTermGradeDataItem.serializer())),
            snapshot,
        )

        assertTrue(resp.isSuccess())
        assertEquals("success", resp.msg)
        assertEquals(1, resp.data?.size)

        val term = resp.data!!.first()
        assertEquals("2024000000", term.studentId)
        assertEquals("张三", term.name)
        assertEquals("2024", term.enrollmentYear)
        assertEquals("120", term.earnedCredits)
        assertEquals("394.3", term.totalGradePoints)
        assertEquals("3.32", term.averageCreditGradePoint)
        assertEquals("", term.averageScore)
        assertTrue(term.semesterGpa.isEmpty())
    }

    @Test
    fun `成绩明细项全字段断言`() {
        val resp = json.decodeFromString(
            JwxtApiResponse.serializer(ListSerializer(JwxtTermGradeDataItem.serializer())),
            snapshot,
        )
        val items = resp.data!!.first().achievement
        assertEquals(5, items.size)

        // 学分整数 + 分数 String
        val english = items.first { it.courseName == "大学英语4" }
        assertEquals("D0001-4", english.courseCode)
        assertEquals("80", english.score)
        assertEquals(3.0, english.gradePoint)
        assertEquals(2.0, english.credit)
        assertEquals("必修", english.curriculumAttributes)
        assertEquals("通识必修", english.courseNature)
        assertEquals("考试", english.examName)
        assertEquals("合格", english.passStatus)
        assertEquals("正常考试", english.examinationNature)
        assertEquals("2025-2026-2", english.semester)
        assertEquals("56F41E75456A9A9AE0630F061ED2C36E", english.gradeId)
        assertEquals("", english.gradeLevel)
        assertEquals("", english.repeatSemester)
        assertEquals("", english.markFlag)

        // 学分小数（3.5）无损解析
        val coa = items.first { it.courseName == "计算机组成原理" }
        assertEquals(3.5, coa.credit)
        assertEquals("70", coa.score)

        // 绩点小数（2.4）无损解析
        val studio = items.first { it.courseName == "工作室课题2" }
        assertEquals(2.4, studio.gradePoint)
        assertEquals("任选", studio.curriculumAttributes)

        // 绩点 4.5 高分段
        val dbCourse = items.first { it.courseName == "数据库与信息管理课程设计" }
        assertEquals(4.5, dbCourse.gradePoint)
        assertEquals("专业实践", dbCourse.courseNature)
        assertEquals("95", dbCourse.score)
    }

    @Test
    fun `新学期空成绩形态`() {
        val resp = json.decodeFromString(
            JwxtApiResponse.serializer(ListSerializer(JwxtTermGradeDataItem.serializer())),
            emptySnapshot,
        )

        assertTrue(resp.isSuccess())
        val term = resp.data!!.single()
        assertTrue(term.achievement.isEmpty())
        assertTrue(term.semesterGpa.isEmpty())
        assertEquals("", term.averageScore)
        // 汇总字段在有累计学分时仍然有值
        assertEquals("394.3", term.totalGradePoints)
        assertEquals("3.32", term.averageCreditGradePoint)
    }
}
