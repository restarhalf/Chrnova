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
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.usecase.DeleteCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveCourseByIdUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveTransCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseBatchUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseWithConflictsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class CourseEditViewModelTest {

    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)

    private val coursesFlow = MutableStateFlow<List<Course>>(emptyList())
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
        teacher: String = "张老师",
        semesterId: String = "2026-1",
        id: Long = 0,
        dayOfWeek: Int = 1,
        startSection: Int = 1,
        sectionCount: Int = 2,
        weeks: List<Int> = listOf(1, 2),
    ) = Course(
        id = id,
        name = name,
        semesterId = semesterId,
        location = "一教",
        teacher = teacher,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        sectionCount = sectionCount,
        weeks = weeks,
        color = "#FF0000",
        type = 0,
    )

    private fun labCourse(
        id: Long = 0,
        name: String = "物理实验",
        dayOfWeek: Int = 3,
        startSection: Int = 5,
        sectionCount: Int = 2,
        weeks: List<Int> = listOf(3, 4),
    ) = Course(
        id = id,
        name = name,
        semesterId = "2026-1",
        location = "实验楼",
        teacher = "李实验",
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        sectionCount = sectionCount,
        weeks = weeks,
        color = "#00FF00",
        type = 1,
        userNo = "20230001",
    )

    private fun makeViewModel(withTrans: Boolean = false): CourseEditViewModel {
        every { courseRepository.observeAllCourses() } returns coursesFlow
        every { auth.observeProfile() } returns profileFlow
        return CourseEditViewModel(
            observeAllCourses = ObserveAllCoursesUseCase(courseRepository),
            observeCourseById = ObserveCourseByIdUseCase(courseRepository),
            saveCourse = SaveCourseUseCase(courseRepository),
            deleteCourseUseCase = DeleteCourseUseCase(courseRepository),
            auth = auth,
            transCourseWithConflicts =
                if (withTrans) {
                    TransCourseWithConflictsUseCase(
                        courseRepository = courseRepository,
                        transCourse = TransCourseUseCase(),
                        transCourseBatch = TransCourseBatchUseCase(TransCourseUseCase()),
                    )
                } else {
                    null
                },
            saveTransCourses = if (withTrans) SaveTransCoursesUseCase(courseRepository) else null,
        )
    }

    private fun TestScope.subscribeUi(vm: CourseEditViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    private suspend fun awaitState(
        vm: CourseEditViewModel,
        timeoutMs: Long = 5000,
        predicate: (CourseEditViewModel.CourseEditUiState) -> Boolean,
    ): CourseEditViewModel.CourseEditUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        while (Clock.System.now().toEpochMilliseconds() < deadline) {
            advanceMain()
            val state = vm.uiState.value
            if (predicate(state)) return state
            withContext(Dispatchers.Default) { delay(10) }
        }
        error("等待 uiState 超时，当前: ${vm.uiState.value}")
    }

    @Test
    fun `uiState初值为空列表`() = runTest {
        val vm = makeViewModel()

        assertTrue(vm.uiState.value.courses.isEmpty())
        assertTrue(vm.uiState.value.courseNames.isEmpty())
    }

    @Test
    fun `uiState响应课程流且courseNames只含同步课`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        coursesFlow.value = listOf(
            syncedCourse("高等数学"),
            syncedCourse("大学物理"),
            labCourse(),
        )

        val state = awaitState(vm) { it.courseNames.isNotEmpty() }
        assertEquals(3, state.courses.size)
        assertEquals(listOf("高等数学", "大学物理"), state.courseNames)
    }

    @Test
    fun `uiState去重同名同步课`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        coursesFlow.value = listOf(
            syncedCourse("高等数学", dayOfWeek = 1),
            syncedCourse("高等数学", dayOfWeek = 2),
        )

        val state = awaitState(vm) { it.courseNames.isNotEmpty() }
        assertEquals(listOf("高等数学"), state.courseNames)
    }

    @Test
    fun `weekdayText转换中文`() {
        val vm = makeViewModel()

        assertEquals("周一", vm.weekdayText(1))
        assertEquals("周三", vm.weekdayText(3))
        assertEquals("周日", vm.weekdayText(7))
    }

    @Test
    fun `weekdayText越界回退周一`() {
        val vm = makeViewModel()

        assertEquals("周一", vm.weekdayText(99))
    }

    @Test
    fun `buildDisabledWeeks聚合时间重叠课程的周次`() {
        val vm = makeViewModel()

        val conflicting = labCourse(id = 9, dayOfWeek = 1, startSection = 5, weeks = listOf(3, 4))

        val otherDay = labCourse(id = 10, dayOfWeek = 2, startSection = 5, weeks = listOf(5))

        val noOverlap = labCourse(id = 11, dayOfWeek = 1, startSection = 1, weeks = listOf(6))

        val disabled = vm.buildDisabledWeeks(
            courses = listOf(conflicting, otherDay, noOverlap),
            dayOfWeek = 1,
            startSection = 5,
            endSection = 6,
            courseId = null,
            editingCourseId = null,
        )

        assertEquals(setOf(3, 4), disabled)
    }

    @Test
    fun `buildDisabledWeeks排除自身课程`() {
        val vm = makeViewModel()

        val self = labCourse(id = 9, dayOfWeek = 1, startSection = 5, weeks = listOf(3, 4))

        val byCourseId = vm.buildDisabledWeeks(
            courses = listOf(self),
            dayOfWeek = 1, startSection = 5, endSection = 6,
            courseId = 9L, editingCourseId = null,
        )
        val byEditingId = vm.buildDisabledWeeks(
            courses = listOf(self),
            dayOfWeek = 1, startSection = 5, endSection = 6,
            courseId = null, editingCourseId = 9L,
        )

        assertTrue(byCourseId.isEmpty())
        assertTrue(byEditingId.isEmpty())
    }

    @Test
    fun `buildLabCourseToSave名称空白返回null`() {
        val vm = makeViewModel()

        assertNull(
            vm.buildLabCourseToSave(
                selectedName = "  ", selectedWeeks = setOf(1),
                classRoom = "一教", teacher = "",
                dayOfWeek = 1, startSection = 1, endSection = 2,
                existing = null, courses = emptyList(),
            ),
        )
    }

    @Test
    fun `buildLabCourseToSave周次为空返回null`() {
        val vm = makeViewModel()

        assertNull(
            vm.buildLabCourseToSave(
                selectedName = "物理实验", selectedWeeks = emptySet(),
                classRoom = "一教", teacher = "",
                dayOfWeek = 1, startSection = 1, endSection = 2,
                existing = null, courses = emptyList(),
            ),
        )
    }

    @Test
    fun `buildLabCourseToSave新建取同步课学期与教师回退`() {
        val vm = makeViewModel()
        val synced = syncedCourse(name = "物理实验")

        val saved = vm.buildLabCourseToSave(
            selectedName = "物理实验", selectedWeeks = setOf(5, 3),
            classRoom = "实验楼", teacher = "  ",
            dayOfWeek = 3, startSection = 5, endSection = 6,
            existing = null, courses = listOf(synced),
        )

        assertEquals("物理实验", saved?.name)
        assertEquals("2026-1", saved?.semesterId)

        assertEquals("张老师", saved?.teacher)
        assertEquals(5, saved?.startSection)
        assertEquals(2, saved?.sectionCount)
        assertEquals(listOf(3, 5), saved?.weeks)
        assertEquals(1, saved?.type)
        assertEquals(0, saved?.id)

        assertEquals("20230001", saved?.userNo)
    }

    @Test
    fun `buildLabCourseToSave教师输入优先于同步课`() {
        val vm = makeViewModel()

        val saved = vm.buildLabCourseToSave(
            selectedName = "物理实验", selectedWeeks = setOf(1),
            classRoom = "实验楼", teacher = "王自定义",
            dayOfWeek = 3, startSection = 5, endSection = 6,
            existing = null, courses = listOf(syncedCourse(name = "物理实验")),
        )

        assertEquals("王自定义", saved?.teacher)
    }

    @Test
    fun `buildLabCourseToSave编辑保留existing字段`() {
        val vm = makeViewModel()
        val existing = labCourse(id = 7, weeks = listOf(1), name = "物理实验").copy(color = "#ABCDEF")

        val saved = vm.buildLabCourseToSave(
            selectedName = "物理实验", selectedWeeks = setOf(2),
            classRoom = "新楼", teacher = "",
            dayOfWeek = 4, startSection = 7, endSection = 8,
            existing = existing, courses = emptyList(),
        )

        assertEquals(7, saved?.id)
        assertEquals("#ABCDEF", saved?.color)
        assertEquals("20230001", saved?.userNo)
        assertEquals("2026-1", saved?.semesterId)

        assertEquals("李实验", saved?.teacher)
    }

    @Test
    fun `buildLabCourseToSave学期回退到首个有学期的同步课`() {
        val vm = makeViewModel()
        val otherCourse = syncedCourse(name = "高等数学", semesterId = "2025-2")

        val saved = vm.buildLabCourseToSave(
            selectedName = "陌生课程", selectedWeeks = setOf(1),
            classRoom = "一教", teacher = "",
            dayOfWeek = 1, startSection = 1, endSection = 2,
            existing = null, courses = listOf(otherCourse),
        )

        assertEquals("2025-2", saved?.semesterId)
    }

    @Test
    fun `buildLabCourseToSave同名同步课学期为空时不回退`() {
        val vm = makeViewModel()
        val noSemester = syncedCourse(name = "陌生课程", semesterId = "")
        val withSemester = syncedCourse(name = "高等数学", semesterId = "2025-2")

        val saved = vm.buildLabCourseToSave(
            selectedName = "陌生课程", selectedWeeks = setOf(1),
            classRoom = "一教", teacher = "",
            dayOfWeek = 1, startSection = 1, endSection = 2,
            existing = null, courses = listOf(noSemester, withSemester),
        )

        assertEquals("", saved?.semesterId)
    }

    @Test
    fun `observeEditingCourse透传到observeCourseById`() = runTest {
        val vm = makeViewModel()
        every { courseRepository.observeCourseById(any()) } returns MutableStateFlow(null)

        vm.observeEditingCourse(42L)

        verify(VerifyMode.exactly(1)) { courseRepository.observeCourseById(42L) }
    }

    @Test
    fun `observeEditingCourse空id回退到-1`() = runTest {
        val vm = makeViewModel()
        every { courseRepository.observeCourseById(any()) } returns MutableStateFlow(null)

        vm.observeEditingCourse(null)

        verify(VerifyMode.exactly(1)) { courseRepository.observeCourseById(-1) }
    }

    @Test
    fun `buildEditingFormState新建用预选值并收敛边界`() {
        val vm = makeViewModel()

        val form = vm.buildEditingFormState(
            courseId = null, editingCourse = null, courseNames = listOf("高数"),
            initialDayOfWeek = 3, initialStartSection = 5, initialSelectedWeek = 2,
        )

        assertFalse(form.isEdit)
        assertEquals(0, form.selectedIndex)
        assertEquals("", form.classRoom)
        assertEquals(3, form.dayOfWeek)
        assertEquals(5, form.startSection)
        assertEquals(5, form.endSection)
        assertEquals(setOf(2), form.selectedWeeks)

        val bounded = vm.buildEditingFormState(
            courseId = null, editingCourse = null, courseNames = emptyList(),
            initialDayOfWeek = 99, initialStartSection = 0, initialSelectedWeek = -1,
        )
        assertEquals(7, bounded.dayOfWeek)
        assertEquals(1, bounded.startSection)
        assertEquals(1, bounded.endSection)
        assertEquals(setOf(1), bounded.selectedWeeks)
    }

    @Test
    fun `buildEditingFormState编辑填充已有数据`() {
        val vm = makeViewModel()
        val editing = labCourse(dayOfWeek = 4, startSection = 7, sectionCount = 2, weeks = listOf(3, 5))

        val form = vm.buildEditingFormState(
            courseId = 7L, editingCourse = editing,
            courseNames = listOf("高等数学", "物理实验"),
        )

        assertTrue(form.isEdit)
        assertEquals(1, form.selectedIndex)
        assertEquals("实验楼", form.classRoom)
        assertEquals("李实验", form.teacher)
        assertEquals(4, form.dayOfWeek)
        assertEquals(7, form.startSection)
        assertEquals(8, form.endSection)
        assertEquals(setOf(3, 5), form.selectedWeeks)
    }

    @Test
    fun `buildEditingFormState编辑模式名称不在列表时回退0`() {
        val vm = makeViewModel()
        val editing = labCourse(name = "不存在的课")

        val form = vm.buildEditingFormState(
            courseId = 7L, editingCourse = editing, courseNames = listOf("高等数学"),
        )

        assertTrue(form.isEdit)
        assertEquals(0, form.selectedIndex)
    }

    @Test
    fun `toggleWeek切换周次选中`() {
        val vm = makeViewModel()

        val added = vm.toggleWeek(setOf(1, 2), 3)
        assertEquals(setOf(1, 2, 3), added)

        val removed = vm.toggleWeek(setOf(1, 2, 3), 2)
        assertEquals(setOf(1, 3), removed)
    }

    @Test
    fun `sectionSummary格式化`() {
        val vm = makeViewModel()

        assertEquals("第3-4节", vm.sectionSummary(3, 4))
    }

    @Test
    fun `saveLabCourse成功回调true`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.insertCourse(any()) } returns 1L

        var callback: Boolean? = null
        vm.saveLabCourse(labCourse()) { callback = it }

        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(true, callback)
        verifySuspend(VerifyMode.exactly(1)) { courseRepository.insertCourse(any()) }
    }

    @Test
    fun `saveLabCourse抛异常回调false`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.insertCourse(any()) } throws RuntimeException("db locked")

        var callback: Boolean? = null
        vm.saveLabCourse(labCourse()) { callback = it }
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(false, callback)
    }

    @Test
    fun `saveLabCourse保存中忽略重复调用`() = runTest {
        val vm = makeViewModel()

        everySuspend { courseRepository.insertCourse(any()) } calls { (c: Course) ->
            withContext(Dispatchers.Default) { delay(200) }
            1L
        }

        var callbacks = 0
        vm.saveLabCourse(labCourse()) { callbacks++ }

        vm.saveLabCourse(labCourse()) { callbacks++ }

        withContext(Dispatchers.Default) { delay(400) }
        advanceMain()

        assertEquals(1, callbacks)
        verifySuspend(VerifyMode.exactly(1)) { courseRepository.insertCourse(any()) }
    }

    @Test
    fun `deleteCourse成功回调true`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.deleteCourse(any()) } returns Unit

        var callback: Boolean? = null
        vm.deleteCourse(labCourse(id = 5)) { callback = it }
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(true, callback)
        verifySuspend(VerifyMode.exactly(1)) { courseRepository.deleteCourse(any()) }
    }

    @Test
    fun `deleteCourse抛异常回调false`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.deleteCourse(any()) } throws RuntimeException("db locked")

        var callback: Boolean? = null
        vm.deleteCourse(labCourse(id = 5)) { callback = it }
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        assertEquals(false, callback)
    }

    @Test
    fun `buildTransFormState预填原课程并默认全选周次`() = runTest {
        val vm = makeViewModel()
        val course =
            Course(
                id = 7,
                name = "实操课",
                location = "实训楼201",
                teacher = "王老师",
                dayOfWeek = 3,
                startSection = 5,
                sectionCount = 2,
                weeks = listOf(1, 2, 4, 6),
                color = "#00FF00",
            )

        val state = vm.buildTransFormState(course)
        assertTrue(state.isReady)
        assertEquals("实训楼201", state.classRoom)
        assertEquals(3, state.dayOfWeek)
        assertEquals(5, state.startSection)
        assertEquals(6, state.endSection)
        assertEquals(setOf(1, 2, 4, 6), state.selectedWeeks)
    }

    @Test
    fun `buildTransFormState课程为空时未就绪`() = runTest {
        val vm = makeViewModel()
        val state = vm.buildTransFormState(null)
        assertFalse(state.isReady)
        assertTrue(state.selectedWeeks.isEmpty())
    }

    @Test
    fun `buildTransBatch按周生成调课记录`() = runTest {
        val vm = makeViewModel(withTrans = true)
        val origin =
            Course(
                id = 7,
                name = "实操课",
                location = "实训楼201",
                teacher = "王老师",
                dayOfWeek = 3,
                startSection = 5,
                sectionCount = 2,
                weeks = listOf(1, 2, 3),
                color = "#00FF00",
                remoteKey = "k7",
            )
        everySuspend { courseRepository.getAllCoursesOnce() } returns listOf(origin)

        val result =
            vm.buildTransBatch(
                originCourse = origin,
                weeks = setOf(1, 2, 3),
                classRoom = "实训楼305",
                dayOfWeek = 5,
                startSection = 1,
                endSection = 2,
            )

        assertNotNull(result)
        assertEquals(listOf(1, 2, 3), result.overrideCourses.map { it.targetWeek })
        assertTrue(result.overrideCourses.all { it.location == "实训楼305" })
        assertTrue(result.overrideCourses.all { it.type == 2 })
        assertFalse(result.hasConflicts)
    }

    @Test
    fun `transMaxWeeks取原课程周数`() {
        val vm = makeViewModel()
        val course =
            Course(
                name = "实操课",
                location = "",
                teacher = "",
                dayOfWeek = 1,
                startSection = 1,
                sectionCount = 2,
                weeks = listOf(1, 2, 4, 6),
                color = "#00FF00",
            )
        assertEquals(4, vm.transMaxWeeks(course))
        assertEquals(1, vm.transMaxWeeks(null))
    }

    @Test
    fun `toggleTransWeek超过上限不再新增`() {
        val vm = makeViewModel()
        val selected = setOf(1, 2, 3, 4)

        assertEquals(setOf(1, 2, 3), vm.toggleTransWeek(selected, 4, maxWeeks = 4))
        assertEquals(selected, vm.toggleTransWeek(selected, 5, maxWeeks = 4))
        assertEquals(setOf(1, 2, 3, 5), vm.toggleTransWeek(setOf(1, 2, 3), 5, maxWeeks = 4))
    }

    @Test
    fun `buildTransBatch无调课依赖时返回null`() = runTest {
        val vm = makeViewModel()
        val course =
            Course(
                name = "实操课",
                location = "",
                teacher = "",
                dayOfWeek = 1,
                startSection = 1,
                sectionCount = 2,
                weeks = listOf(1),
                color = "#00FF00",
            )
        assertNull(
            vm.buildTransBatch(course, setOf(1), "", 2, 1, 2)
        )
    }

    @Test
    fun `saveTransBatch批量写入`() = runTest {
        val vm = makeViewModel(withTrans = true)
        val origin =
            Course(
                name = "实操课",
                location = "实训楼201",
                teacher = "王老师",
                dayOfWeek = 3,
                startSection = 5,
                sectionCount = 2,
                weeks = listOf(1),
                color = "#00FF00",
                remoteKey = "k7",
            )
        val override =
            origin.copy(
                id = 0,
                type = 2,
                originRemoteKey = "k7",
                targetWeek = 1,
                weeks = listOf(1),
            )
        everySuspend { courseRepository.getAllCoursesOnce() } returns listOf(origin)

        assertTrue(vm.saveTransBatch(origin, listOf(override)))
        verifySuspend { courseRepository.replaceCourses(emptyList(), listOf(override)) }
    }

}
