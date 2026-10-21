package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.JwxtSelectionClassification
import restarhalf.stellar.schedule.domain.model.JwxtSelectionCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionOperResult
import restarhalf.stellar.schedule.domain.model.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.port.CourseSelectionPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.JwxtSelectionDropResult
import restarhalf.stellar.schedule.domain.port.JwxtSelectionSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CourseSelectionUseCaseTest {

    private val gateway = mock<CourseSelectionPort>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val useCase = CourseSelectionUseCase(gateway, authWorkflow)

    private val ctx = CourseSelectionUseCase.SessionContext(
        rotationId = "r1",
        sessionTime = "st-123",
        classifications = listOf(JwxtSelectionClassification(classificationCode = "01", classificationName = "必修")),
        extraRules = mapOf("courseQualification" to "false"),
    )

    private fun session(
        sessionTime: String = "st-123",
        classifications: List<JwxtSelectionClassification> = emptyList(),
        extraRules: Map<String, String> = emptyMap(),
    ) = JwxtSelectionSession(
        sessionTime = sessionTime,
        classifications = classifications,
        extraRules = extraRules,
    )

    @Test
    fun `loadRotations成功返回轮次并确保已登录`() = runTest {
        everySuspend { gateway.fetchRotations() } returns listOf(
            JwxtSelectionRotation(rotationId = "r1", rotationName = "第一轮"),
        )

        val rotations = useCase.loadRotations()

        assertEquals(1, rotations.size)
        assertEquals("r1", rotations[0].rotationId)
        assertEquals("第一轮", rotations[0].rotationName)
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.ensureLoggedIn() }
        verifySuspend(VerifyMode.exactly(1)) { gateway.fetchRotations() }
    }

    @Test
    fun `loadRotations透传端口异常`() = runTest {
        everySuspend { gateway.fetchRotations() } throws IllegalStateException("还有讲课学时需要选")

        val e = runCatching { useCase.loadRotations() }.exceptionOrNull()

        assertIs<IllegalStateException>(e)
        assertEquals("还有讲课学时需要选", e.message)
    }

    @Test
    fun `initSession映射会话字段`() = runTest {
        everySuspend { gateway.initSession("r1") } returns session(
            sessionTime = "st-123",
            classifications = listOf(JwxtSelectionClassification(classificationCode = "01", classificationName = "必修")),
            extraRules = mapOf("compulsorySelection" to "true", "courseQualification" to "false"),
        )

        val context = useCase.initSession("r1")

        assertEquals("r1", context.rotationId)
        assertEquals("st-123", context.sessionTime)
        assertEquals(1, context.classifications.size)
        assertEquals("01", context.classifications[0].classificationCode)
        assertEquals("true", context.extraRules["compulsorySelection"])
        assertEquals("false", context.extraRules["courseQualification"])
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.ensureLoggedIn() }
    }

    @Test
    fun `loadCourses透传上下文与搜索词`() = runTest {
        everySuspend { gateway.fetchCourses(any(), any(), any(), any(), any()) } returns listOf(
            JwxtSelectionCourse(courseName = "高数", courseId = "c1", noticeId = "n1"),
        )

        val courses = useCase.loadCourses(ctx, classificationCode = "01", courseInformation = "高数")

        assertEquals(1, courses.size)
        assertEquals("c1", courses[0].courseId)
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.fetchCourses("r1", "01", "st-123", mapOf("courseQualification" to "false"), "高数")
        }
    }

    @Test
    fun `submitOnce首次成功直接返回`() = runTest {
        everySuspend {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns JwxtSelectionOperResult.Success("选课成功")

        val result = useCase.submitOnce(
            ctx,
            classificationCode = "01",
            course = JwxtSelectionCourse(
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
        everySuspend { gateway.fetchCourses(any(), any(), any(), any(), any()) } returns listOf(
            JwxtSelectionCourse(courseName = "关联班", courseId = "c2", noticeId = "n2"),
        )

        val result = useCase.submitOnce(
            ctx,
            classificationCode = "01",
            course = JwxtSelectionCourse(
                courseName = "高数", courseId = "c1", noticeId = "n1",
            ),
        )

        assertTrue(result is JwxtSelectionOperResult.Success)
        assertEquals(2, submitCalls)

        verifySuspend(VerifyMode.exactly(1)) {
            gateway.submitSelection("r1", "c2", "n2", "st-123", "01", any(), "n1", any(), any())
        }
    }

    @Test
    fun `submitOnce未找到关联教学班时返回NeedConfirm`() = runTest {
        everySuspend {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns JwxtSelectionOperResult.NeedConfirm("还有关联课", yxcfbs = "y1", cfbs = "c1", xkkcid = "x1", yxjx0404id = "n404")
        everySuspend { gateway.fetchCourses(any(), any(), any(), any(), any()) } returns listOf(
            JwxtSelectionCourse(courseName = "无关课", courseId = "c9", noticeId = "n9"),
        )

        val result = useCase.submitOnce(
            ctx,
            classificationCode = "01",
            course = JwxtSelectionCourse(
                courseName = "高数", courseId = "c1", noticeId = "n1",
            ),
        )

        assertIs<JwxtSelectionOperResult.NeedConfirm>(result)
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `submitOnce重新加载课程列表失败时返回NeedConfirm`() = runTest {
        everySuspend {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns JwxtSelectionOperResult.NeedConfirm("还有关联课", yxcfbs = "y1", cfbs = "c1", xkkcid = "x1", yxjx0404id = "n2")
        everySuspend { gateway.fetchCourses(any(), any(), any(), any(), any()) } throws
            IllegalStateException("网络异常")

        val result = useCase.submitOnce(
            ctx,
            classificationCode = "01",
            course = JwxtSelectionCourse(
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
            JwxtSelectionDropResult(success = true, message = "")

        useCase.drop(ctx, noticeId = "n1")

        verifySuspend(VerifyMode.exactly(1)) { gateway.dropSelection("r1", "n1", "st-123", "false") }
    }

    @Test
    fun `drop无规则位时使用默认true`() = runTest {
        everySuspend { gateway.dropSelection(any(), any(), any(), any()) } returns
            JwxtSelectionDropResult(success = true, message = "")
        val ctxNoRule = ctx.copy(extraRules = emptyMap())

        useCase.drop(ctxNoRule, noticeId = "n1")

        verifySuspend(VerifyMode.exactly(1)) { gateway.dropSelection("r1", "n1", "st-123", "true") }
    }

    @Test
    fun `loadSelectedCourses返回已选课程`() = runTest {
        everySuspend { gateway.fetchSelectedCourses(any()) } returns listOf(
            restarhalf.stellar.schedule.domain.model.JwxtSelectedCourse(
                courseName = "高数", noticeId = "n1", isCanTk = "1",
            ),
        )

        val selected = useCase.loadSelectedCourses(ctx)

        assertEquals(1, selected.size)
        assertEquals("1", selected[0].isCanTk)
        verifySuspend(VerifyMode.exactly(1)) { gateway.fetchSelectedCourses("r1") }
    }

    @Test
    fun `refreshSession刷新会话后重新初始化`() = runTest {
        everySuspend { gateway.initSession("r1") } returns session(sessionTime = "st-456")

        val context = useCase.refreshSession("r1")

        assertEquals("st-456", context.sessionTime)
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
        verifySuspend(VerifyMode.exactly(1)) { gateway.initSession("r1") }
    }
}
