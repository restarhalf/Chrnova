package restarhalf.stellar.schedule.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.put
import restarhalf.stellar.schedule.domain.model.agent.ClientCommandTypeDto

@Serializable
data class McpTool(
    val name: String,
    val inputSchema: JsonObject,
)

class ToolsRegistry {
    private val tools = linkedMapOf<String, McpTool>()

    fun registerTool(name: String, inputSchema: JsonObject) {
        tools[name] = McpTool(name = name, inputSchema = inputSchema)
    }

    fun listTools(): List<McpTool> = tools.values.toList()

    fun commandTypeFor(name: String): ClientCommandTypeDto? = when (name) {
        "get_courses" -> ClientCommandTypeDto.GET_COURSES
        "get_grades" -> ClientCommandTypeDto.GET_GRADES
        "get_exams" -> ClientCommandTypeDto.GET_EXAMS
        "add_lab_course" -> ClientCommandTypeDto.ADD_LAB_COURSE
        "transfer_course" -> ClientCommandTypeDto.TRANSFER_COURSE
        "refresh_timetable" -> ClientCommandTypeDto.RUN_SYNC
        "set_theme_mode" -> ClientCommandTypeDto.SET_THEME_MODE
        "set_floating_bar" -> ClientCommandTypeDto.SET_FLOATING_BAR
        "set_show_non_current_week" -> ClientCommandTypeDto.SET_SHOW_NON_CURRENT_WEEK
        "set_course_reminder_enabled" -> ClientCommandTypeDto.SET_COURSE_REMINDER_ENABLED
        "set_exam_reminder_enabled" -> ClientCommandTypeDto.SET_EXAM_REMINDER_ENABLED
        "set_campus" -> ClientCommandTypeDto.SET_CAMPUS
        "set_term_start" -> ClientCommandTypeDto.SET_TERM_START
        "set_total_weeks" -> ClientCommandTypeDto.SET_TOTAL_WEEKS
        else -> null
    }

    fun registerDefaultTools() {
        commandNames.forEach { name ->
            registerTool(
                name,
                buildJsonObject {
                    put($$"$schema", "https://json-schema.org/draft/2020-12/schema")
                    put("type", "object")
                    putJsonObject("properties") {
                        schemaProperties[name]?.forEach { (key, type) ->
                            val description = schemaDescriptions[name]?.get(key)
                            put(
                                key,
                                buildJsonObject {
                                    put("type", type)
                                    if (!description.isNullOrBlank()) {
                                        put("description", description)
                                    }
                                },
                            )
                        }
                    }
                    val required = schemaRequired[name].orEmpty()
                    if (required.isNotEmpty()) {
                        put("required", kotlinx.serialization.json.JsonArray(required.map(::JsonPrimitive)))
                    }
                    put("additionalProperties", buildJsonObject { put("type", "string") })
                },
            )
        }
    }

    private val schemaProperties = mapOf(
        "get_courses" to mapOf("week" to "string", "weekOffset" to "string"),
        "get_grades" to mapOf("semester" to "string"),
        "get_exams" to mapOf("semester" to "string", "nameOrNumber" to "string"),
        "add_lab_course" to mapOf(
            "name" to "string",
            "dayOfWeek" to "string",
            "startSection" to "string",
            "sectionCount" to "string",
            "weeks" to "string",
            "location" to "string",
            "teacher" to "string",
        ),
        "transfer_course" to mapOf(
            "course" to "string",
            "dayOfWeek" to "string",
            "startSection" to "string",
            "sectionCount" to "string",
            "weeks" to "string",
            "location" to "string",
        ),
        "set_theme_mode" to mapOf("value" to "string"),
        "set_floating_bar" to mapOf("value" to "string"),
        "set_show_non_current_week" to mapOf("value" to "string"),
        "set_course_reminder_enabled" to mapOf("value" to "string"),
        "set_exam_reminder_enabled" to mapOf("value" to "string"),
        "set_campus" to mapOf("value" to "string"),
        "set_term_start" to mapOf("value" to "string"),
        "set_total_weeks" to mapOf("value" to "string"),
    )

    private val schemaDescriptions = mapOf(
        "get_courses" to mapOf(
            "week" to "指定周次，如 15（优先于 weekOffset）",
            "weekOffset" to "周偏移：0=本周，1=下周，-1=上周",
        ),
        "get_grades" to mapOf("semester" to "学期，如 2025-2026-2，可留空为当前学期"),
        "get_exams" to mapOf(
            "semester" to "学期，可留空为当前学期",
            "nameOrNumber" to "课程名或课程号，可留空",
        ),
        "add_lab_course" to mapOf(
            "name" to "课程名称",
            "dayOfWeek" to "星期：1=周一，7=周日",
            "startSection" to "开始节次(1-20)",
            "sectionCount" to "节数(1-10)",
            "weeks" to "周次，如 1-16 或 1,3,5",
            "location" to "地点",
            "teacher" to "教师",
        ),
        "transfer_course" to mapOf(
            "course" to "课程名称或课程 id",
            "dayOfWeek" to "星期：1=周一，7=周日",
            "startSection" to "开始节次(1-20)",
            "sectionCount" to "节数(1-8)",
            "weeks" to "周次，如 1-16 或 1,3,5",
            "location" to "地点",
        ),
        "set_theme_mode" to mapOf("value" to "0=跟随系统，1=浅色，2=深色"),
        "set_floating_bar" to mapOf("value" to "0=固定，1=悬浮，2=液态玻璃"),
        "set_show_non_current_week" to mapOf("value" to "是否显示非本周课程：true/false"),
        "set_course_reminder_enabled" to mapOf("value" to "课程提醒开关：true/false"),
        "set_exam_reminder_enabled" to mapOf("value" to "考试提醒开关：true/false"),
        "set_campus" to mapOf("value" to "校区名称，如 开发区/金石滩"),
        "set_term_start" to mapOf("value" to "开学日期(yyyy-MM-dd)或时间戳(ms)"),
        "set_total_weeks" to mapOf("value" to "总周数(1-20)"),
    )

    private val schemaRequired = mapOf(
        "add_lab_course" to listOf("name", "dayOfWeek", "startSection", "sectionCount", "weeks"),
        "transfer_course" to listOf("course"),
        "set_total_weeks" to listOf("value"),
    )

    private val commandNames = listOf(
        "get_courses",
        "get_grades",
        "get_exams",
        "add_lab_course",
        "transfer_course",
        "refresh_timetable",
        "set_theme_mode",
        "set_floating_bar",
        "set_show_non_current_week",
        "set_course_reminder_enabled",
        "set_exam_reminder_enabled",
        "set_campus",
        "set_term_start",
        "set_total_weeks",
    )
}
