package restarhalf.stellar.schedule.data.remote

/**
 * 教务系统课程时间解析器
 * 
 * 负责将教务系统返回的课程时间字符串解析为结构化数据。
 * 支持解析星期、节次、周次等信息。
 */
object JwxtTimeParser {

    /**
     * 解析后的课程数据
     * 
     * @param name 课程名称
     * @param location 上课地点
     * @param teacher 教师
     * @param dayOfWeek 星期几（1-7）
     * @param startSection 开始节次
     * @param sectionCount 持续节数
     * @param weeks 上课周次列表
     * @param color 课程颜色
     * @param type 课程类型（0=普通，1=实验，2=调课）
     * @param remoteKey 远程标识
     * @param originRemoteKey 原始远程标识（调课时使用）
     * @param targetWeek 目标周次（调课时使用）
     */
    data class ParsedCourse(
        val name: String,
        val location: String,
        val teacher: String,
        val dayOfWeek: Int,
        val startSection: Int,
        val sectionCount: Int,
        val weeks: List<Int>,
        val color: String,
        val type: Int = 0,
        val remoteKey: String = "",
        val originRemoteKey: String? = null,
        val targetWeek: Int = 0,
    )

    /**
     * 课程时间槽
     * 
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param sectionCount 持续节数
     * @param weeks 周次列表
     */
    data class Slot(
        val dayOfWeek: Int,
        val startSection: Int,
        val sectionCount: Int,
        val weeks: List<Int>
    )

    /**
     * 将课程项解析为课程列表
     * 
     * @param item 教务系统课程项
     * @param color 课程颜色
     * @return 解析后的课程列表
     */
    fun parseToCourses(item: JwxtCurriculumItem, color: String = "#E6E6FA"): List<ParsedCourse> {
        val slot = parseSlot(item) ?: return emptyList()
        return listOf(
            ParsedCourse(
                name = item.courseName,
                location = item.location.trim(),
                teacher = item.teacherName,
                dayOfWeek = slot.dayOfWeek,
                startSection = slot.startSection,
                sectionCount = slot.sectionCount,
                weeks = slot.weeks,
                color = color,
                type = 0,
                remoteKey = item.kch.trim() + "|" + item.classTime.trim()
            )
        )
    }

    /**
     * 解析课程时间槽
     * 
     * @param item 课程项
     * @return 时间槽，解析失败返回null
     */
    private fun parseSlot(item: JwxtCurriculumItem): Slot? {
        val dayOfWeek = parseDayOfWeek(item.classTime) ?: return null
        val (startSection, sectionCount) = parseSections(item.classTime) ?: return null
        val weeks = parseWeeks(item)
        if (weeks.isEmpty()) return null
        return Slot(
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            sectionCount = sectionCount,
            weeks = weeks
        )
    }

    /**
     * 解析星期几
     * 
     * @param classTime 时间字符串（如"10102"表示周一第1-2节）
     * @return 星期几（1-7），解析失败返回null
     */
    private fun parseDayOfWeek(classTime: String): Int? {
        val trimmed = classTime.trim()
        if (trimmed.isBlank()) return null
        val firstChar = trimmed.firstOrNull() ?: return null
        val day = firstChar.digitToIntOrNull() ?: return null
        return day.takeIf { it in 1..7 }
    }

    /**
     * 解析节次信息
     * 
     * @param classTime 时间字符串
     * @return 开始节次和持续节数，解析失败返回null
     */
    private fun parseSections(classTime: String): Pair<Int, Int>? {
        val trimmed = classTime.trim()
        if (trimmed.length < 5) return null

        val sectionPart = trimmed.substring(1)
        if (sectionPart.length % 2 != 0) return null
        val sectionNumbers =
            sectionPart.chunked(2).mapNotNull { it.toIntOrNull() }.filter { it in 1..30 }

        if (sectionNumbers.isEmpty()) return null
        val startSection = sectionNumbers.minOrNull() ?: return null
        return startSection to sectionNumbers.size
    }

    /**
     * 解析周次信息
     * 
     * @param item 课程项
     * @return 周次列表
     */
    private fun parseWeeks(item: JwxtCurriculumItem): List<Int> {
        // 优先使用classWeekDetails（逗号分隔的周次列表）
        val details = item.classWeekDetails.trim()
        if (details.isNotBlank()) {
            val weekNumbers =
                details.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
            if (weekNumbers.isNotEmpty()) return weekNumbers.distinct().sorted()
        }

        // 其次使用classWeek表达式（如"1-8,10-16"）
        val expression = item.classWeek.trim()
        if (expression.isBlank()) return emptyList()
        return expandWeekExpression(expression)
    }

    /**
     * 展开周次表达式
     * 
     * 支持格式：
     * - "1-8" -> [1,2,3,4,5,6,7,8]
     * - "1,3,5" -> [1,3,5]
     * - "1-8,10-16" -> [1,2,3,4,5,6,7,8,10,11,12,13,14,15,16]
     * 
     * @param expression 周次表达式
     * @return 展开后的周次列表
     */
    private fun expandWeekExpression(expression: String): List<Int> {
        val cleaned = expression.replace("，", ",").replace("、", ",")
        val weekSet = linkedSetOf<Int>()

        cleaned
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { token ->
                if (token.contains('-')) {
                    val (startStr, endStr) = token.split('-', limit = 2)
                    val start = startStr.trim().toIntOrNull()
                    val end = endStr.trim().toIntOrNull()
                    if (start != null && end != null) {
                        val range = if (start <= end) start..end else end..start
                        range.forEach { weekSet.add(it) }
                    }
                } else {
                    token.toIntOrNull()?.let { weekSet.add(it) }
                }
            }

        return weekSet.toList().sorted()
    }
}
