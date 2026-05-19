package restarhalf.stellar.schedule.agent.control

import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.core.course.effectiveCoursesForWeek
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.agent.ClientCommandDto
import restarhalf.stellar.schedule.domain.model.agent.ClientCommandResultRequest
import restarhalf.stellar.schedule.domain.model.agent.ClientCommandTypeDto
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.InsertCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.RunSyncUseCase
import restarhalf.stellar.schedule.domain.usecase.GetTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.GetTotalWeeksUseCase
import restarhalf.stellar.schedule.domain.usecase.SetCourseReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.SetExamReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.SetFloatingBarUseCase
import restarhalf.stellar.schedule.domain.usecase.SetShowNonCurrentWeekUseCase
import restarhalf.stellar.schedule.domain.usecase.SetTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.SetThemeModeUseCase
import restarhalf.stellar.schedule.domain.usecase.SetTotalWeeksUseCase

class ClientCommandExecutor(
    private val courseRepository: CourseRepository,
    private val fetchGrades: FetchGradesSimpleUseCase,
    private val fetchExaminations: FetchExaminationsSimpleUseCase,
    private val insertCourse: InsertCourseUseCase,
    private val runSync: RunSyncUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setFloatingBar: SetFloatingBarUseCase,
    private val setShowNonCurrentWeek: SetShowNonCurrentWeekUseCase,
    private val setCourseReminderEnabled: SetCourseReminderEnabledUseCase,
    private val setExamReminderEnabled: SetExamReminderEnabledUseCase,
    private val getTermStartMs: GetTermStartMsUseCase,
    private val getTotalWeeks: GetTotalWeeksUseCase,
    private val setTermStartMs: SetTermStartMsUseCase,
    private val setTotalWeeks: SetTotalWeeksUseCase,
    private val json: Json,
) {
    private val memory = mutableListOf<String>()

    suspend fun execute(command: ClientCommandDto): ClientCommandResultRequest =
        runCatching {
            when (command.type) {
                ClientCommandTypeDto.GET_COURSES -> {
                    val allCourses = courseRepository.getAllCoursesOnce()
                    val totalWeeks = getTotalWeeks()
                    val termStartMs = getTermStartMs()
                    val weekInfo =
                        WeekCalculator.detect(totalWeeks = totalWeeks, termStartMs = termStartMs)
                    val offset = command.arguments["weekOffset"].orEmpty().toIntOrNull() ?: 0
                    val baseWeek = weekInfo.week + offset
                    val week = baseWeek.coerceIn(1, totalWeeks)
                    val courses =
                        if (weekInfo.isHoliday && offset == 0) {
                            emptyList()
                        } else {
                            val effective = effectiveCoursesForWeek(all = allCourses, week = week)
                            effective.filter { isCourseActiveInWeek(it, week) }
                        }
                    json.encodeToString(courses)
                }
                ClientCommandTypeDto.GET_GRADES -> json.encodeToString(fetchGrades(command.arguments["semester"].orEmpty()))
                ClientCommandTypeDto.GET_EXAMS -> json.encodeToString(
                    fetchExaminations(
                        semester = command.arguments["semester"].orEmpty(),
                        nameOrNumber = command.arguments["nameOrNumber"].orEmpty(),
                    ),
                )
                ClientCommandTypeDto.ADD_LAB_COURSE -> addLabCourse(command)
                ClientCommandTypeDto.TRANSFER_COURSE -> transferCourse(command)
                ClientCommandTypeDto.SCHEDULE_REMINDER -> "已收到提醒：${command.arguments["title"].orEmpty()} @ ${command.arguments["triggerAt"].orEmpty()}"
                ClientCommandTypeDto.CANCEL_REMINDER -> "当前没有可取消的 Agent 通用提醒；课程/考试提醒可在设置中关闭。"
                ClientCommandTypeDto.NAVIGATE -> "NAVIGATE:${command.arguments["screen"].orEmpty()}"
                ClientCommandTypeDto.RUN_SYNC -> json.encodeToString(runSync())
                ClientCommandTypeDto.SET_THEME_MODE -> {
                    setThemeMode(command.arguments["value"].orEmpty().toIntOrNull() ?: 0)
                    "主题模式已更新"
                }
                ClientCommandTypeDto.SET_FLOATING_BAR -> {
                    setFloatingBar(command.arguments["value"].orEmpty().toIntOrNull() ?: 0)
                    "悬浮栏设置已更新"
                }
                ClientCommandTypeDto.SET_SHOW_NON_CURRENT_WEEK -> {
                    setShowNonCurrentWeek(command.arguments["value"].orEmpty().toBooleanStrictOrNull() ?: false)
                    "非当前周显示设置已更新"
                }
                ClientCommandTypeDto.SET_COURSE_REMINDER_ENABLED -> {
                    setCourseReminderEnabled(command.arguments["value"].orEmpty().toBooleanStrictOrNull() ?: false)
                    "课程提醒设置已更新"
                }
                ClientCommandTypeDto.SET_EXAM_REMINDER_ENABLED -> {
                    setExamReminderEnabled(command.arguments["value"].orEmpty().toBooleanStrictOrNull() ?: false)
                    "考试提醒设置已更新"
                }
                ClientCommandTypeDto.SET_TERM_START -> {
                    setTermStartMs(command.arguments["value"].orEmpty().toLongOrNull() ?: 0L)
                    "开学时间已更新"
                }
                ClientCommandTypeDto.SET_TOTAL_WEEKS -> {
                    val weeks = command.arguments["value"].orEmpty().toIntOrNull()
                        ?: throw IllegalArgumentException("总周数必须是数字")
                    require(weeks in 1..30) { "总周数必须在 1 到 30 之间" }
                    setTotalWeeks(weeks)
                    "总周数已更新"
                }
                ClientCommandTypeDto.SET_CAMPUS -> "校区设置需要客户端页面确认，已收到：${command.arguments["value"].orEmpty()}"
                ClientCommandTypeDto.REMEMBER -> {
                    val fact = command.arguments["fact"].orEmpty()
                    if (fact.isNotBlank()) memory += fact
                    "已记住：$fact"
                }
                ClientCommandTypeDto.RECALL_MEMORY -> memory.joinToString("\n").ifBlank { "暂无客户端临时记忆" }
            }
        }.fold(
            onSuccess = { payload -> ClientCommandResultRequest(commandId = command.id, success = true, payload = payload) },
            onFailure = { throwable ->
                ClientCommandResultRequest(
                    commandId = command.id,
                    success = false,
                    error = throwable.message ?: "客户端工具执行失败",
                )
            },
        )

    private suspend fun addLabCourse(command: ClientCommandDto): String {
        val name = command.arguments["name"].orEmpty().trim()
        require(name.isNotBlank()) { "实验课名称不能为空" }
        val dayOfWeek = requiredInt(command, "dayOfWeek", "星期")
        require(dayOfWeek in 1..7) { "星期必须在 1 到 7 之间" }
        val startSection = requiredInt(command, "startSection", "开始节次")
        require(startSection in 1..20) { "开始节次必须在 1 到 20 之间" }
        val sectionCount = requiredInt(command, "sectionCount", "节数")
        require(sectionCount in 1..8) { "节数必须在 1 到 8 之间" }
        val weeks = parseWeeks(command.arguments["weeks"].orEmpty())
        require(weeks.isNotEmpty()) { "周次不能为空" }
        val course = Course(
            name = name,
            location = command.arguments["location"].orEmpty(),
            teacher = command.arguments["teacher"].orEmpty(),
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            sectionCount = sectionCount,
            weeks = weeks,
            color = "#4F8BFF",
            type = 1,
        )
        val id = insertCourse(course)
        return "实验课已添加，id=$id"
    }

    private suspend fun transferCourse(command: ClientCommandDto): String {
        val courseName = command.arguments["course"].orEmpty().trim()
        require(courseName.isNotBlank()) { "调课课程不能为空" }
        val source = courseRepository.getAllCoursesOnce().firstOrNull { it.name == courseName || it.id.toString() == courseName }
            ?: throw IllegalArgumentException("未找到要调课的课程：$courseName")
        val weeks = parseWeeks(command.arguments["weeks"].orEmpty())
        val updated = source.copy(
            dayOfWeek = command.arguments["dayOfWeek"].orEmpty().toIntOrNull() ?: source.dayOfWeek,
            startSection = command.arguments["startSection"].orEmpty().toIntOrNull() ?: source.startSection,
            sectionCount = command.arguments["sectionCount"].orEmpty().toIntOrNull() ?: source.sectionCount,
            weeks = weeks.ifEmpty { source.weeks },
            location = command.arguments["location"].orEmpty().ifBlank { source.location },
            id = 0,
            type = 2,
            originRemoteKey = source.remoteKey,
            remoteKey = source.remoteKey + "#agent-transfer#" + command.id,
        )
        val id = insertCourse(updated)
        return "调课已添加为覆盖课程，id=$id"
    }

    private fun parseWeeks(raw: String): List<Int> =
        raw.split(',', '，', ' ')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()

    private fun requiredInt(command: ClientCommandDto, key: String, label: String): Int =
        command.arguments[key].orEmpty().toIntOrNull()
            ?: throw IllegalArgumentException("$label 必须是数字")
}

