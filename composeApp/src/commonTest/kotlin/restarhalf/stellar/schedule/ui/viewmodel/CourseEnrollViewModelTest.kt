package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.JwxtSelectedCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionClassification
import restarhalf.stellar.schedule.domain.model.JwxtSelectionCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionOperResult
import restarhalf.stellar.schedule.domain.model.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.port.CourseSelectionPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.JwxtSelectionDropResult
import restarhalf.stellar.schedule.domain.port.JwxtSelectionSession
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CourseEnrollViewModelTest {

    private val port = mock<CourseSelectionPort>(MockMode.autofill)
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

    private fun makeViewModel() = CourseEnrollViewModel(
        useCase = CourseSelectionUseCase(port, authWorkflow),
    )

    private fun stubRotations() {
        everySuspend { port.fetchRotations() } returns listOf(
            JwxtSelectionRotation(rotationId = "r1", rotationName = "第一轮"),
        )
    }

    private fun stubSession(rotationId: String = "r1") {
        everySuspend { port.initSession(rotationId) } returns JwxtSelectionSession(
            sessionTime = "st-1",
            classifications = listOf(JwxtSelectionClassification(classificationCode = "c1", classificationName = "通识")),
            extraRules = emptyMap(),
        )
    }

    private fun stubCourses(courses: List<JwxtSelectionCourse> = emptyList()) {
        everySuspend {
            port.fetchCourses(any(), any(), any(), any(), any())
        } returns courses
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
        everySuspend { port.fetchSelectedCourses(any()) } returns emptyList()
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
        stubCourses(listOf(selectionCourse()))
        everySuspend { port.fetchSelectedCourses(any()) } calls {
            if (selectedHasCourse) {
                listOf(JwxtSelectedCourse(courseName = "高等数学", noticeId = "n1"))
            } else {
                emptyList()
            }
        }
        everySuspend {
            port.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
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
        everySuspend { port.fetchSelectedCourses(any()) } returns emptyList()
        everySuspend {
            port.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
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
        everySuspend { port.fetchSelectedCourses(any()) } returns listOf(
            JwxtSelectedCourse(courseName = "高等数学", noticeId = "n1", isCanTk = "1"),
        )
        everySuspend { port.dropSelection(any(), any(), any(), any()) } returns
            JwxtSelectionDropResult(success = true, message = "")

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
