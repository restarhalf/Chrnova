package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.usecase.DeleteExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveExaminationUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * ExamEditViewModel 单元测试。
 *
 * Save/DeleteExaminationUseCase 是 final class，用真实实例 + mock repository 走完整委托链。
 * selectedTerm/currentTerm 是 Eagerly stateIn，构造后直接读 value 即可。
 */
class ExamEditViewModelTest {

    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)

    private val coursesFlow = MutableStateFlow<List<Course>>(emptyList())
    private val selectedTermFlow = MutableStateFlow("")
    private val currentTermFlow = MutableStateFlow("2026-1")
    private val profileFlow = MutableStateFlow(JwxtAuthProfile(userNo = "20230001"))

    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun syncedCourse(
        name: String,
        remoteKey: String = "CS101",
    ) = Course(
        name = name,
        location = "一教",
        teacher = "张老师",
        dayOfWeek = 1,
        startSection = 1,
        sectionCount = 2,
        weeks = listOf(1),
        color = "#FF0000",
        type = 0,
        remoteKey = remoteKey,
    )

    private fun examination(
        id: Long = 0,
        courseNumber: String = "CS101",
        courseName: String = "高等数学",
        time: String = "2026-01-15 09:00-11:00",
        source: String = "jwxt",
    ) = Examination(
        id = id,
        courseNumber = courseNumber,
        courseName = courseName,
        time = time,
        examinationPlace = "一教101",
        zwh = "12",
        ksbz = "带计算器",
        source = source,
        userNo = "20230001",
    )

    private fun makeViewModel(): ExamEditViewModel {
        every { courseRepository.observeAllCourses() } returns coursesFlow
        every { settings.observeSelectedTerm() } returns selectedTermFlow
        every { settings.observeCurrentTermId() } returns currentTermFlow
        every { auth.observeProfile() } returns profileFlow
        return ExamEditViewModel(
            courseRepository = courseRepository,
            examinationRepository = examinationRepository,
            auth = auth,
            settings = settings,
            academic = academic,
            saveExaminationUseCase = SaveExaminationUseCase(examinationRepository),
            deleteExaminationUseCase = DeleteExaminationUseCase(examinationRepository),
        )
    }

    private fun TestScope.subscribeUi(vm: ExamEditViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    private suspend fun awaitState(
        vm: ExamEditViewModel,
        timeoutMs: Long = 5000,
        predicate: (ExamEditViewModel.ExamEditUiState) -> Boolean,
    ): ExamEditViewModel.ExamEditUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        while (Clock.System.now().toEpochMilliseconds() < deadline) {
            advanceMain()
            val state = vm.uiState.value
            if (predicate(state)) return state
            withContext(Dispatchers.Default) { delay(10) }
        }
        error("等待 uiState 超时，当前: ${vm.uiState.value}")
    }

    // region uiState 与学期流

    @Test
    fun `uiState初值为空列表`() = runTest {
        val vm = makeViewModel()

        assertTrue(vm.uiState.value.courseNames.isEmpty())
        assertTrue(vm.uiState.value.courses.isEmpty())
    }

    @Test
    fun `uiState响应课程流`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        coursesFlow.value = listOf(syncedCourse("高等数学"), syncedCourse("大学物理"))

        val state = awaitState(vm) { it.courseNames.isNotEmpty() }
        assertEquals(listOf("高等数学", "大学物理"), state.courseNames)
        assertEquals(2, state.courses.size)
    }

    @Test
    fun `selectedTerm响应选中学期流`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        assertEquals("", vm.selectedTerm.value)

        selectedTermFlow.value = "2026-2"
        advanceMain()

        assertEquals("2026-2", vm.selectedTerm.value)
    }

    @Test
    fun `currentTerm响应当前学期流`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        assertEquals("2026-1", vm.currentTerm.value)

        currentTermFlow.value = "2026-2"
        advanceMain()

        assertEquals("2026-2", vm.currentTerm.value)
    }

    // endregion

    // region 纯函数

    @Test
    fun `buildEditingFormState新建返回空白表单`() {
        val vm = makeViewModel()

        val form = vm.buildEditingFormState(examinationId = null, editingExamination = null, courseNames = listOf("高数"))

        assertFalse(form.isEdit)
        assertEquals(0, form.selectedIndex)
        assertEquals("", form.courseNumber)
        assertEquals("", form.courseName)
        assertEquals("", form.datePart)
        assertEquals("", form.timePart)
        assertEquals("", form.examinationPlace)
        assertEquals("", form.zwh)
        assertEquals("", form.ksbz)
    }

    @Test
    fun `buildEditingFormState编辑拆分时间与填充`() {
        val vm = makeViewModel()

        val form = vm.buildEditingFormState(
            examinationId = 3L,
            editingExamination = examination(time = "2026-01-15 09:00-11:00"),
            courseNames = listOf("高等数学", "大学物理"),
        )

        assertTrue(form.isEdit)
        assertEquals(0, form.selectedIndex)
        assertEquals("CS101", form.courseNumber)
        assertEquals("高等数学", form.courseName)
        assertEquals("2026-01-15", form.datePart)
        assertEquals("09:00-11:00", form.timePart)
        assertEquals("一教101", form.examinationPlace)
        assertEquals("12", form.zwh)
        assertEquals("带计算器", form.ksbz)
    }

    @Test
    fun `buildEditingFormState仅日期时timePart为空`() {
        val vm = makeViewModel()

        val form = vm.buildEditingFormState(
            examinationId = 3L,
            editingExamination = examination(time = "2026-01-15"),
            courseNames = emptyList(),
        )

        assertEquals("2026-01-15", form.datePart)
        assertEquals("", form.timePart)
    }

    @Test
    fun `buildEditingFormState名称不在列表时回退0`() {
        val vm = makeViewModel()

        val form = vm.buildEditingFormState(
            examinationId = 3L,
            editingExamination = examination(courseName = "陌生课程"),
            courseNames = listOf("高等数学"),
        )

        assertEquals(0, form.selectedIndex)
    }

    @Test
    fun `buildExaminationToSave课程名空白返回null`() {
        val vm = makeViewModel()

        assertNull(
            vm.buildExaminationToSave(
                courseName = " ", datePart = "2026-01-15", timePart = "09:00",
                examinationPlace = "", zwh = "", ksbz = "",
                courses = emptyList(), existing = null,
            ),
        )
    }

    @Test
    fun `buildExaminationToSave时间拼接三形态`() {
        val vm = makeViewModel()

        val both = vm.buildExaminationToSave(
            courseName = "高等数学", datePart = "2026-01-15", timePart = "09:00-11:00",
            examinationPlace = "", zwh = "", ksbz = "", courses = emptyList(), existing = null,
        )
        val dateOnly = vm.buildExaminationToSave(
            courseName = "高等数学", datePart = "2026-01-15", timePart = "",
            examinationPlace = "", zwh = "", ksbz = "", courses = emptyList(), existing = null,
        )
        val timeOnly = vm.buildExaminationToSave(
            courseName = "高等数学", datePart = "", timePart = "09:00-11:00",
            examinationPlace = "", zwh = "", ksbz = "", courses = emptyList(), existing = null,
        )

        assertEquals("2026-01-15 09:00-11:00", both?.time)
        assertEquals("2026-01-15", dateOnly?.time)
        assertEquals("09:00-11:00", timeOnly?.time)
    }

    @Test
    fun `buildExaminationToSave编号existing优先于课程remoteKey`() {
        val vm = makeViewModel()
        val existing = examination(courseNumber = "OLD99")

        val saved = vm.buildExaminationToSave(
            courseName = "高等数学", datePart = "2026-01-15", timePart = "",
            examinationPlace = "一教101", zwh = "1", ksbz = "",
            courses = listOf(syncedCourse("高等数学", remoteKey = "CS101")),
            existing = existing,
        )

        assertEquals("OLD99", saved?.courseNumber)
    }

    @Test
    fun `buildExaminationToSave编号回退remoteKey再回退EXAM哈希`() {
        val vm = makeViewModel()

        val fromRemoteKey = vm.buildExaminationToSave(
            courseName = "高等数学", datePart = "", timePart = "",
            examinationPlace = "", zwh = "", ksbz = "",
            courses = listOf(syncedCourse("高等数学", remoteKey = "CS101")),
            existing = null,
        )
        val fromHash = vm.buildExaminationToSave(
            courseName = "陌生课程", datePart = "", timePart = "",
            examinationPlace = "", zwh = "", ksbz = "",
            courses = listOf(syncedCourse("高等数学", remoteKey = "")),
            existing = null,
        )

        assertEquals("CS101", fromRemoteKey?.courseNumber)
        assertEquals("EXAM_${"陌生课程".hashCode().toUInt()}", fromHash?.courseNumber)
    }

    @Test
    fun `buildExaminationToSave编辑保留existing字段`() {
        val vm = makeViewModel()

        val saved = vm.buildExaminationToSave(
            courseName = "高等数学", datePart = "2026-01-16", timePart = "14:00-16:00",
            examinationPlace = "新楼", zwh = "8", ksbz = "改过",
            courses = emptyList(),
            existing = examination(id = 7, source = "jwxt"),
        )

        assertEquals(7, saved?.id)
        assertEquals("jwxt", saved?.source)
        assertEquals("20230001", saved?.userNo)
        assertEquals("2026-01-16 14:00-16:00", saved?.time)
    }

    @Test
    fun `buildExaminationToSave新建source为manual且userNo取档案`() {
        val vm = makeViewModel()

        val saved = vm.buildExaminationToSave(
            courseName = "高等数学", datePart = "", timePart = "",
            examinationPlace = "", zwh = "", ksbz = "",
            courses = emptyList(), existing = null,
        )

        assertEquals("manual", saved?.source)
        assertEquals("20230001", saved?.userNo)
    }

    @Test
    fun `observeEditingExamination透传与空id回退`() = runTest {
        val vm = makeViewModel()
        every { examinationRepository.observeExaminationById(any()) } returns MutableStateFlow(null)

        vm.observeEditingExamination(9L)
        vm.observeEditingExamination(null)

        verify(VerifyMode.exactly(1)) { examinationRepository.observeExaminationById(9L) }
        verify(VerifyMode.exactly(1)) { examinationRepository.observeExaminationById(-1) }
    }

    // endregion

    // region saveExamination / deleteExamination

    @Test
    fun `saveExamination成功且学期取选中学期`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        selectedTermFlow.value = "2026-2"
        advanceMain()
        everySuspend { examinationRepository.saveExamination(any(), any()) } returns 1L

        var callback: Boolean? = null
        vm.saveExamination(examination()) { callback = it }
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(true, callback)
        verifySuspend(VerifyMode.exactly(1)) {
            examinationRepository.saveExamination(any(), "2026-2")
        }
    }

    @Test
    fun `saveExamination无选中学期回退fetchCurrentTermId`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        // selectedTerm=""，currentTerm 即使有值也应优先 fetchCurrentTermId 结果
        everySuspend { academic.fetchCurrentTermId() } returns "2027-1"
        everySuspend { examinationRepository.saveExamination(any(), any()) } returns 1L

        var callback: Boolean? = null
        vm.saveExamination(examination()) { callback = it }
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(true, callback)
        verifySuspend(VerifyMode.exactly(1)) {
            examinationRepository.saveExamination(any(), "2027-1")
        }
    }

    @Test
    fun `saveExaminationfetchTerm失败回退当前学期`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        everySuspend { academic.fetchCurrentTermId() } throws RuntimeException("网络故障")
        everySuspend { examinationRepository.saveExamination(any(), any()) } returns 1L

        var callback: Boolean? = null
        vm.saveExamination(examination()) { callback = it }
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(true, callback)
        verifySuspend(VerifyMode.exactly(1)) {
            examinationRepository.saveExamination(any(), "2026-1")
        }
    }

    @Test
    fun `saveExamination抛异常回调false`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        everySuspend { examinationRepository.saveExamination(any(), any()) } throws RuntimeException("db locked")

        var callback: Boolean? = null
        vm.saveExamination(examination()) { callback = it }
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(false, callback)
    }

    @Test
    fun `saveExamination保存中忽略重复调用`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        everySuspend { examinationRepository.saveExamination(any(), any()) } calls { (e: Examination) ->
            withContext(Dispatchers.Default) { delay(200) }
            1L
        }

        var callbacks = 0
        vm.saveExamination(examination()) { callbacks++ }
        vm.saveExamination(examination()) { callbacks++ }

        withContext(Dispatchers.Default) { delay(400) }
        advanceMain()

        assertEquals(1, callbacks)
        verifySuspend(VerifyMode.exactly(1)) { examinationRepository.saveExamination(any(), any()) }
    }

    @Test
    fun `deleteExamination成功回调true`() = runTest {
        val vm = makeViewModel()
        everySuspend { examinationRepository.deleteExamination(any()) } returns Unit

        var callback: Boolean? = null
        vm.deleteExamination(5L) { callback = it }
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(true, callback)
        verifySuspend(VerifyMode.exactly(1)) { examinationRepository.deleteExamination(5L) }
    }

    @Test
    fun `deleteExamination抛异常回调false`() = runTest {
        val vm = makeViewModel()
        everySuspend { examinationRepository.deleteExamination(any()) } throws RuntimeException("db locked")

        var callback: Boolean? = null
        vm.deleteExamination(5L) { callback = it }
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(false, callback)
    }

    // endregion
}
