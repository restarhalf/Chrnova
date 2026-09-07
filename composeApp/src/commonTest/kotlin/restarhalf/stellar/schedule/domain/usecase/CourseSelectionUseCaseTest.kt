package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtSelectionClassification
import restarhalf.stellar.schedule.data.remote.JwxtSelectionOperResult
import restarhalf.stellar.schedule.data.remote.JwxtSelectionResponse
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CourseSelectionUseCaseTest {

    private val gateway = mock<JwxtGateway>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val useCase = CourseSelectionUseCase(gateway, authWorkflow)

    private val json = Json { ignoreUnknownKeys = true }

    private val ctx = CourseSelectionUseCase.SessionContext(
        rotationId = "r1",
        sessionTime = "st-123",
        classifications = listOf(JwxtSelectionClassification(classificationCode = "01", classificationName = "必修")),
        extraRules = mapOf("courseQualification" to "false"),
    )

    private fun ok(data: String) = JwxtSelectionResponse(errorCode = "success", data = json.parseToJsonElement(data))

    @Test
    fun `loadRotations成功解析轮次并确保已登录`() = runTest {
        everySuspend { gateway.fetchSelectionRotations(any()) } returns
            ok("""[{"rotationid":"r1","rotationname":"第一轮"}]""")

        val rotations = useCase.loadRotations()

        assertEquals(1, rotations.size)
        assertEquals("r1", rotations[0].rotationId)
        assertEquals("第一轮", rotations[0].rotationName)
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.ensureLoggedIn() }
        verifySuspend(VerifyMode.exactly(1)) { gateway.fetchSelectionRotations(1) }
    }

    @Test
    fun `loadRotations失败时抛出解析后的错误消息`() = runTest {
        everySuspend { gateway.fetchSelectionRotations(any()) } returns JwxtSelectionResponse(
            errorCode = "fail",
            errorMessage = "还有[0]需要选",
            errorMessageParam = listOf("讲课学时"),
        )

        val e = assertFailsWith<IllegalStateException> { useCase.loadRotations() }

        assertEquals("还有讲课学时需要选", e.message)
    }

    @Test
    fun `loadRotations失败且消息为空时使用默认文案`() = runTest {
        everySuspend { gateway.fetchSelectionRotations(any()) } returns JwxtSelectionResponse(errorCode = "fail")

        val e = assertFailsWith<IllegalStateException> { useCase.loadRotations() }

        assertEquals("获取选课轮次失败", e.message)
    }

    @Test
    fun `initSession解析sessionTime分类与规则位`() = runTest {
        everySuspend { gateway.initSelectionSession("r1") } returns ok(
            """{"sessionTime":"st-123","classificationList":[{"classificationCode":"01","classificationName":"必修"}],""" +
                """"compulsorySelection":true,"courseQualification":"false"}"""
        )

        val context = useCase.initSession("r1")

        assertEquals("r1", context.rotationId)
        assertEquals("st-123", context.sessionTime)
        assertEquals(1, context.classifications.size)
        assertEquals("01", context.classifications[0].classificationCode)
        assertEquals("true", context.extraRules["compulsorySelection"])
        assertEquals("false", context.extraRules["courseQualification"])
    }

    @Test
    fun `initSession缺少sessionTime时抛出异常`() = runTest {
        everySuspend { gateway.initSelectionSession("r1") } returns ok("""{"foo":1}""")

        val e = assertFailsWith<IllegalStateException> { useCase.initSession("r1") }

        assertEquals("会话初始化失败：未获取到 sessionTime", e.message)
    }

    @Test
    fun `loadCourses透传上下文与搜索词`() = runTest {
        everySuspend { gateway.fetchSelectionCourses(any(), any(), any(), any(), any()) } returns
            ok("""[{"courseName":"高数","courseId":"c1","noticeId":"n1"}]""")

        val courses = useCase.loadCourses(ctx, classificationCode = "01", courseInformation = "高数")

        assertEquals(1, courses.size)
        assertEquals("c1", courses[0].courseId)
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.fetchSelectionCourses("r1", "01", "st-123", mapOf("courseQualification" to "false"), "高数")
        }
    }

    @Test
    fun `loadCourses失败且消息为空时使用默认文案`() = runTest {
        everySuspend { gateway.fetchSelectionCourses(any(), any(), any(), any(), any()) } returns
            JwxtSelectionResponse(errorCode = "fail")

        val e = assertFailsWith<IllegalStateException> { useCase.loadCourses(ctx, "01") }

        assertEquals("获取课程列表失败", e.message)
    }

    @Test
    fun `submitOnce首次成功直接返回`() = runTest {
        everySuspend {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns JwxtSelectionOperResult.Success("选课成功")

        val result = useCase.submitOnce(
            ctx,
            classificationCode = "01",
            course = restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse(
                courseName = "高数", courseId = "c1", noticeId = "n1",
            ),
        )

        assertIs<JwxtSelectionOperResult.Success>(result)
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.submitSelection("r1", "c1", "n1", "st-123", "01", "", "", "", any())
        }
    }

    @Test
    fun `submitOnce需要确认时自动选关联教学班`() = runTest {
        var submitCalls = 0
        // CallArgs 解构最多支持 7 个参数，submitSelection 有 9 个参数，此处不解构
        everySuspend {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } calls {
            submitCalls++
            if (submitCalls == 1) {
                JwxtSelectionOperResult.NeedConfirm("还有关联课", yxcfbs = "y1", cfbs = "c1", xkkcid = "x1", yxjx0404id = "n2")
            } else {
                JwxtSelectionOperResult.Success("选课成功")
            }
        }
        everySuspend { gateway.fetchSelectionCourses(any(), any(), any(), any(), any()) } returns
            ok("""[{"courseName":"关联班","courseId":"c2","noticeId":"n2"}]""")

        val result = useCase.submitOnce(
            ctx,
            classificationCode = "01",
            course = restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse(
                courseName = "高数", courseId = "c1", noticeId = "n1",
            ),
        )

        assertTrue(result is JwxtSelectionOperResult.Success)
        assertEquals(2, submitCalls)
        // 第二次提交携带关联教学班 n2，并把第一次的 n1 作为已选教学班回传
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.submitSelection("r1", "c2", "n2", "st-123", "01", any(), "n1", any(), any())
        }
    }

    @Test
    fun `submitOnce未找到关联教学班时返回NeedConfirm`() = runTest {
        everySuspend {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns JwxtSelectionOperResult.NeedConfirm("还有关联课", yxcfbs = "y1", cfbs = "c1", xkkcid = "x1", yxjx0404id = "n404")
        everySuspend { gateway.fetchSelectionCourses(any(), any(), any(), any(), any()) } returns
            ok("""[{"courseName":"无关课","courseId":"c9","noticeId":"n9"}]""")

        val result = useCase.submitOnce(
            ctx,
            classificationCode = "01",
            course = restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse(
                courseName = "高数", courseId = "c1", noticeId = "n1",
            ),
        )

        assertIs<JwxtSelectionOperResult.NeedConfirm>(result)
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `drop透传courseQualification规则位`() = runTest {
        everySuspend { gateway.dropSelection(any(), any(), any(), any()) } returns
            JwxtSelectionResponse(errorCode = "success")

        useCase.drop(ctx, noticeId = "n1")

        verifySuspend(VerifyMode.exactly(1)) { gateway.dropSelection("r1", "n1", "st-123", "false") }
    }

    @Test
    fun `drop无规则位时使用默认true`() = runTest {
        everySuspend { gateway.dropSelection(any(), any(), any(), any()) } returns
            JwxtSelectionResponse(errorCode = "success")
        val ctxNoRule = ctx.copy(extraRules = emptyMap())

        useCase.drop(ctxNoRule, noticeId = "n1")

        verifySuspend(VerifyMode.exactly(1)) { gateway.dropSelection("r1", "n1", "st-123", "true") }
    }

    @Test
    fun `loadSelectedCourses解析已选课程`() = runTest {
        everySuspend { gateway.fetchSelectedCourses(any()) } returns
            ok("""[{"courseName":"高数","noticeId":"n1","isCanTk":"1"}]""")

        val selected = useCase.loadSelectedCourses(ctx)

        assertEquals(1, selected.size)
        assertEquals("1", selected[0].isCanTk)
        verifySuspend(VerifyMode.exactly(1)) { gateway.fetchSelectedCourses("r1") }
    }

    @Test
    fun `refreshSession刷新会话后重新初始化`() = runTest {
        everySuspend { gateway.initSelectionSession("r1") } returns ok("""{"sessionTime":"st-456"}""")

        val context = useCase.refreshSession("r1")

        assertEquals("st-456", context.sessionTime)
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
        verifySuspend(VerifyMode.exactly(1)) { gateway.initSelectionSession("r1") }
    }
}
