package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtSelectedCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionOperResult
import restarhalf.stellar.schedule.data.remote.JwxtSelectionResponse
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CourseEnrollViewModel 单元测试。
 *
 * CourseSelectionUseCase 为真实实例 + mock JwxtGateway；
 * session 通过 selectRotation → initSelectionSession 成功响应建立。
 */
class CourseEnrollViewModelTest {

    private val gateway = mock<JwxtGateway>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val mainDispatcher = UnconfinedTestDispatcher()
    private var selectedHasCourse = false

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        selectedHasCourse = false
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun okResponse(dataJson: String? = null) = JwxtSelectionResponse(
        errorCode = "success",
        data = dataJson?.let { Json.parseToJsonElement(it) },
    )

    private fun makeViewModel() = CourseEnrollViewModel(
        useCase = CourseSelectionUseCase(gateway, authWorkflow),
    )

    private fun stubRotations() {
        everySuspend { gateway.fetchSelectionRotations(any()) } returns okResponse(
            """[{"rotationid":"r1","rotationname":"第一轮"}]""",
        )
    }

    private fun stubSession(rotationId: String = "r1") {
        everySuspend { gateway.initSelectionSession(rotationId) } returns okResponse(
            """{"sessionTime":"st-1","classificationList":[
                {"classificationCode":"c1","classificationName":"通识"}]}""",
        )
    }

    private fun stubCourses(jsonArray: String = "[]") {
        everySuspend {
            gateway.fetchSelectionCourses(any(), any(), any(), any(), any())
        } returns okResponse(jsonArray)
    }

    private fun selectionCourse(
        courseId: String = "k1",
        noticeId: String = "n1",
        name: String = "高等数学",
    ) = JwxtSelectionCourse(
        courseId = courseId,
        noticeId = noticeId,
        courseName = name,
        kxh = "01",
    )

    @Test
    fun `加载轮次并自动进入首个轮次`() = runTest {
        stubRotations()
        stubSession()
        everySuspend { gateway.fetchSelectedCourses(any()) } returns okResponse("[]")
        stubCourses()

        val vm = makeViewModel()
        vm.loadRotations()
        advanceMain()

        val state = vm.uiState.value
        assertEquals(1, state.rotations.size)
        assertEquals("r1", state.selectedRotationId)
        assertTrue(state.sessionReady)
        assertEquals("c1", state.selectedClassificationCode)
    }

    @Test
    fun `选课成功写入notice并刷新已选`() = runTest {
        stubRotations()
        stubSession()
        stubCourses("""[{"courseId":"k1","noticeId":"n1","courseName":"高等数学","kxh":"01"}]""")
        everySuspend { gateway.fetchSelectedCourses(any()) } calls {
            okResponse(
                if (selectedHasCourse) {
                    """[{"courseName":"高等数学","noticeId":"n1"}]"""
                } else {
                    "[]"
                },
            )
        }
        everySuspend {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } calls {
            selectedHasCourse = true
            JwxtSelectionOperResult.Success("选课成功")
        }

        val vm = makeViewModel()
        vm.loadRotations()
        advanceMain()
        vm.selectCourse(selectionCourse())
        advanceMain()

        val state = vm.uiState.value
        assertTrue(state.notice.contains("选课成功"))
        assertEquals(1, state.selectedCourses.size)
    }

    @Test
    fun `选课失败写入error`() = runTest {
        stubRotations()
        stubSession()
        stubCourses()
        everySuspend { gateway.fetchSelectedCourses(any()) } returns okResponse("[]")
        everySuspend {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns JwxtSelectionOperResult.Fail("课容量已满")

        val vm = makeViewModel()
        vm.loadRotations()
        advanceMain()
        vm.selectCourse(selectionCourse())
        advanceMain()

        assertEquals("课容量已满", vm.uiState.value.error)
    }

    @Test
    fun `退课成功从已选列表移除`() = runTest {
        stubRotations()
        stubSession()
        stubCourses()
        everySuspend { gateway.fetchSelectedCourses(any()) } returns okResponse(
            """[{"courseName":"高等数学","noticeId":"n1","isCanTk":"1"}]""",
        )
        everySuspend { gateway.dropSelection(any(), any(), any(), any()) } returns okResponse()

        val vm = makeViewModel()
        vm.loadRotations()
        advanceMain()
        assertEquals(1, vm.uiState.value.selectedCourses.size)

        vm.dropCourse(JwxtSelectedCourse(courseName = "高等数学", noticeId = "n1", isCanTk = "1"))
        advanceMain()

        val state = vm.uiState.value
        assertTrue(state.notice.contains("退课成功"))
        assertTrue(state.selectedCourses.isEmpty())
    }
}
