package restarhalf.stellar.schedule.data.remote

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JwxtCourseSelectionAdapterTest {

    private val gateway = mock<JwxtGateway>(MockMode.autofill)
    private val adapter = JwxtCourseSelectionAdapter(gateway)

    private val json = Json { ignoreUnknownKeys = true }

    private fun ok(data: String? = null) = JwxtSelectionResponse(
        errorCode = "success",
        data = data?.let { json.parseToJsonElement(it) },
    )

    @Test
    fun `fetchRotations解析轮次`() = runTest {
        everySuspend { gateway.fetchSelectionRotations(any()) } returns ok(
            """[{"rotationid":"r1","rotationname":"第一轮"}]""",
        )

        val rotations = adapter.fetchRotations()

        assertEquals(1, rotations.size)
        assertEquals("r1", rotations[0].rotationId)
    }

    @Test
    fun `fetchRotations失败时抛出解析后的错误消息`() = runTest {
        everySuspend { gateway.fetchSelectionRotations(any()) } returns JwxtSelectionResponse(
            errorCode = "fail",
            errorMessage = "还有[0]需要选",
            errorMessageParam = listOf("讲课学时"),
        )

        val e = assertFailsWith<IllegalStateException> { adapter.fetchRotations() }

        assertEquals("还有讲课学时需要选", e.message)
    }

    @Test
    fun `fetchRotations失败且消息为空时使用默认文案`() = runTest {
        everySuspend { gateway.fetchSelectionRotations(any()) } returns JwxtSelectionResponse(errorCode = "fail")

        val e = assertFailsWith<IllegalStateException> { adapter.fetchRotations() }

        assertEquals("获取选课轮次失败", e.message)
    }

    @Test
    fun `initSession解析会话`() = runTest {
        everySuspend { gateway.initSelectionSession("r1") } returns ok("""{"sessionTime":"st-123"}""")

        val session = adapter.initSession("r1")

        assertEquals("st-123", session.sessionTime)
    }

    @Test
    fun `initSession失败且消息为空时使用默认文案`() = runTest {
        everySuspend { gateway.initSelectionSession("r1") } returns JwxtSelectionResponse(errorCode = "fail")

        val e = assertFailsWith<IllegalStateException> { adapter.initSession("r1") }

        assertEquals("进入选课失败", e.message)
    }

    @Test
    fun `fetchCourses透传参数并解析课程`() = runTest {
        everySuspend { gateway.fetchSelectionCourses(any(), any(), any(), any(), any()) } returns ok(
            """[{"courseName":"高数","courseId":"c1","noticeId":"n1"}]""",
        )

        val courses = adapter.fetchCourses(
            rotationId = "r1",
            classificationCode = "01",
            sessionTime = "st-123",
            extraRules = mapOf("courseQualification" to "false"),
            courseInformation = "高数",
        )

        assertEquals(1, courses.size)
        assertEquals("c1", courses[0].courseId)
    }

    @Test
    fun `fetchCourses失败且消息为空时使用默认文案`() = runTest {
        everySuspend { gateway.fetchSelectionCourses(any(), any(), any(), any(), any()) } returns
            JwxtSelectionResponse(errorCode = "fail")

        val e = assertFailsWith<IllegalStateException> { adapter.fetchCourses("r1", "01", "st-123", emptyMap()) }

        assertEquals("获取课程列表失败", e.message)
    }

    @Test
    fun `dropSelection映射成功与消息`() = runTest {
        everySuspend { gateway.dropSelection(any(), any(), any(), any()) } returns JwxtSelectionResponse(
            errorCode = "success",
            errorMessage = "退课成功",
        )

        val result = adapter.dropSelection("r1", "n1", "st-123", "true")

        assertTrue(result.success)
        assertEquals("退课成功", result.message)
    }

    @Test
    fun `dropSelection失败时保留服务端消息`() = runTest {
        everySuspend { gateway.dropSelection(any(), any(), any(), any()) } returns JwxtSelectionResponse(
            errorCode = "fail",
            errorMessage = "不在退课时间内",
        )

        val result = adapter.dropSelection("r1", "n1", "st-123", "true")

        assertEquals(false, result.success)
        assertEquals("不在退课时间内", result.message)
    }

    @Test
    fun `fetchSelectedCourses解析已选课程`() = runTest {
        everySuspend { gateway.fetchSelectedCourses(any()) } returns ok(
            """[{"courseName":"高数","noticeId":"n1","isCanTk":"1"}]""",
        )

        val selected = adapter.fetchSelectedCourses("r1")

        assertEquals(1, selected.size)
        assertEquals("1", selected[0].isCanTk)
    }

    @Test
    fun `fetchSelectedCourses失败且消息为空时使用默认文案`() = runTest {
        everySuspend { gateway.fetchSelectedCourses(any()) } returns JwxtSelectionResponse(errorCode = "fail")

        val e = assertFailsWith<IllegalStateException> { adapter.fetchSelectedCourses("r1") }

        assertEquals("获取已选课程失败", e.message)
    }
}
