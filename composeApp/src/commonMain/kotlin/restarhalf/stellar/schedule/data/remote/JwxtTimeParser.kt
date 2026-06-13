package restarhalf.stellar.schedule.data.remote

object JwxtTimeParser {

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

    data class Slot(
        val dayOfWeek: Int,
        val startSection: Int,
        val sectionCount: Int,
        val weeks: List<Int>
    )

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

    private fun parseDayOfWeek(classTime: String): Int? {
        val trimmed = classTime.trim()
        if (trimmed.isBlank()) return null
        val firstChar = trimmed.firstOrNull() ?: return null
        val day = firstChar.digitToIntOrNull() ?: return null
        return day.takeIf { it in 1..7 }
    }

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

    private fun parseWeeks(item: JwxtCurriculumItem): List<Int> {

        val details = item.classWeekDetails.trim()
        if (details.isNotBlank()) {
            val weekNumbers =
                details.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
            if (weekNumbers.isNotEmpty()) return weekNumbers.distinct().sorted()
        }


        val expression = item.classWeek.trim()
        if (expression.isBlank()) return emptyList()
        return expandWeekExpression(expression)
    }

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
