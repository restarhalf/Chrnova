package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.model.JwxtSelectionClassification
import restarhalf.stellar.schedule.domain.model.JwxtSelectionCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionRotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JwxtSelectionParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun data(raw: String) = json.parseToJsonElement(raw)

    @Test
    fun `parseList解析轮次数组`() {
        val rotations = JwxtSelectionParser.parseList(
            data("""[{"rotationid":"r1","rotationname":"第一轮"}]"""),
            JwxtSelectionRotation.serializer(),
            "选课轮次列表",
        )

        assertEquals(1, rotations.size)
        assertEquals("r1", rotations[0].rotationId)
        assertEquals("第一轮", rotations[0].rotationName)
    }

    @Test
    fun `parseList数据为空时返回空列表`() {
        val rotations = JwxtSelectionParser.parseList(
            null,
            JwxtSelectionRotation.serializer(),
            "选课轮次列表",
        )

        assertTrue(rotations.isEmpty())
    }

    @Test
    fun `parseList结构异常时返回空列表`() {
        val courses = JwxtSelectionParser.parseList(
            data("""{"foo":1}"""),
            JwxtSelectionCourse.serializer(),
            "课程列表",
        )

        assertTrue(courses.isEmpty())
    }

    @Test
    fun `parseSessionContext解析sessionTime分类与规则位`() {
        val session = JwxtSelectionParser.parseSessionContext(
            data(
                """{"sessionTime":"st-123","classificationList":[{"classificationCode":"01","classificationName":"必修"}],""" +
                    """"compulsorySelection":true,"courseQualification":"false"}""",
            ),
        )

        assertEquals("st-123", session.sessionTime)
        assertEquals(1, session.classifications.size)
        assertEquals("01", session.classifications[0].classificationCode)
        assertEquals("true", session.extraRules["compulsorySelection"])
        assertEquals("false", session.extraRules["courseQualification"])
    }

    @Test
    fun `parseSessionContext过滤非布尔规则位`() {
        val session = JwxtSelectionParser.parseSessionContext(
            data("""{"sessionTime":"st-1","compulsoryGrades":"yes","selectionGrades":true}"""),
        )

        assertEquals(setOf("selectionGrades"), session.extraRules.keys)
        assertEquals("true", session.extraRules["selectionGrades"])
    }

    @Test
    fun `parseSessionContext非对象响应抛出格式异常`() {
        val e = assertFailsWith<IllegalStateException> {
            JwxtSelectionParser.parseSessionContext(data("""[1,2,3]"""))
        }

        assertEquals("会话初始化失败：响应格式异常", e.message)
    }

    @Test
    fun `parseSessionContext缺少sessionTime时抛出异常`() {
        val e = assertFailsWith<IllegalStateException> {
            JwxtSelectionParser.parseSessionContext(data("""{"foo":1}"""))
        }

        assertEquals("会话初始化失败：未获取到 sessionTime", e.message)
    }

    @Test
    fun `parseSessionContext分类列表异常时降级为空`() {
        val session = JwxtSelectionParser.parseSessionContext(
            data("""{"sessionTime":"st-1","classificationList":"oops"}"""),
        )

        assertEquals("st-1", session.sessionTime)
        assertTrue(session.classifications.isEmpty())
    }

    @Test
    fun `requireSuccess失败时替换错误消息参数`() {
        val resp = JwxtSelectionResponse(
            errorCode = "fail",
            errorMessage = "还有[0]需要选",
            errorMessageParam = listOf("讲课学时"),
        )

        val e = assertFailsWith<IllegalStateException> {
            JwxtSelectionParser.requireSuccess(resp, "获取选课轮次失败")
        }

        assertEquals("还有讲课学时需要选", e.message)
    }

    @Test
    fun `requireSuccess失败且消息为空时使用默认文案`() {
        val resp = JwxtSelectionResponse(errorCode = "fail")

        val e = assertFailsWith<IllegalStateException> {
            JwxtSelectionParser.requireSuccess(resp, "获取选课轮次失败")
        }

        assertEquals("获取选课轮次失败", e.message)
    }

    @Test
    fun `requireSuccess成功时不抛异常`() {
        JwxtSelectionParser.requireSuccess(JwxtSelectionResponse(errorCode = "success"), "获取选课轮次失败")
    }

    @Test
    fun `parseList保留分类字段`() {
        val classifications = JwxtSelectionParser.parseList(
            data("""[{"classificationCode":"c1","classificationName":"通识"}]"""),
            JwxtSelectionClassification.serializer(),
            "选课分类列表",
        )

        assertEquals(1, classifications.size)
        assertEquals("c1", classifications[0].classificationCode)
        assertEquals("通识", classifications[0].classificationName)
    }
}
