package restarhalf.stellar.schedule.core.text

import restarhalf.stellar.schedule.domain.model.Course

object CsvExporter {

    fun export(courses: List<Course>): String {
        val sb = StringBuilder()
        sb.appendLine("课程名称,星期,开始节数,结束节数,老师,地点,周数")
        for (course in courses) {
            val name = escapeCsv(course.name)
            val day = course.dayOfWeek
            val start = course.startSection
            val end = course.startSection + course.sectionCount - 1
            val teacher = escapeCsv(course.teacher)
            val location = escapeCsv(course.location)
            val weeks = formatWeeksForCsv(course.weeks)
            sb.appendLine("$name,$day,$start,$end,$teacher,$location,$weeks")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun formatWeeksForCsv(weeks: List<Int>): String {
        if (weeks.isEmpty()) return ""
        val sorted = weeks.sorted()
        val result = mutableListOf<String>()

        var index = 0
        while (index < sorted.size) {
            val startWeek = sorted[index]
            var endWeek = startWeek
            var step = 0

            if (index + 1 < sorted.size) {
                val nextWeek = sorted[index + 1]
                if (nextWeek == startWeek + 1 || nextWeek == startWeek + 2) {
                    step = nextWeek - startWeek
                    endWeek = nextWeek
                    var nextIndex = index + 2
                    while (nextIndex < sorted.size && sorted[nextIndex] == endWeek + step) {
                        endWeek = sorted[nextIndex]
                        nextIndex++
                    }

                    if (step == 2) {
                        val suffix = if (startWeek % 2 == 1) "单" else "双"
                        result.add("${startWeek}-${endWeek}$suffix")
                        index = nextIndex
                        continue
                    } else if (step == 1) {
                        result.add("${startWeek}-${endWeek}")
                        index = nextIndex
                        continue
                    }
                }
            }

            result.add(startWeek.toString())
            index++
        }

        return result.joinToString("、")
    }
}
