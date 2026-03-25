package restarhalf.stellar.schedule.core.text

object WeeksFormatter {
    fun format(weeks: List<Int>): String {
        if (weeks.isEmpty()) return ""
        val sorted = weeks.sorted()
        val result = mutableListOf<String>()

        var index = 0
        while (index < sorted.size) {
            val startWeek = sorted[index]
            var endWeek = startWeek
            var count = 1
            var step = 0


            if (index + 1 < sorted.size) {
                val nextWeek = sorted[index + 1]
                if (nextWeek == startWeek + 1 || nextWeek == startWeek + 2) {
                    step = nextWeek - startWeek
                    endWeek = nextWeek
                    count = 2
                    var nextIndex = index + 2
                    while (nextIndex < sorted.size && sorted[nextIndex] == endWeek + step) {
                        endWeek = sorted[nextIndex]
                        count++
                        nextIndex++
                    }


                    if (step == 2 && count >= 3) {
                        val type = if (startWeek % 2 == 0) "(双周)" else "(单周)"
                        result.add("${startWeek}-${endWeek}周$type")
                        index = nextIndex
                        continue
                    } else if (step == 1) {

                        result.add("${startWeek}-${endWeek}周")
                        index = nextIndex
                        continue
                    }
                }
            }


            if (startWeek == endWeek) {
                result.add("${startWeek}周")
            } else {


                result.add("${startWeek}周")
            }
            index++
        }

        return result.joinToString("、")
    }
}
