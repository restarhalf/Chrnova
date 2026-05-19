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
                            put(key, buildJsonObject { put("type", type) })
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
        "get_courses" to mapOf("weekOffset" to "string"),
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
