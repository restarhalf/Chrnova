package restarhalf.stellar.schedule.core.text

/**
 * 周次格式化工具对象
 * 
 * 将周次列表格式化为简洁的显示文本，支持连续周次的合并显示。
 * 例如：[1,2,3,5,7,9,11] -> "1-3周、5周、7周、9周、11周"
 * [2,4,6,8,10] -> "2-10周(双周)"
 */
object WeeksFormatter {
    /**
     * 将周次列表格式化为显示文本
     * 
     * @param weeks 周次列表（如[1,2,3,5,7,9]）
     * @return 格式化后的字符串（如"1-3周、5周、7-9周"）
     */
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

            // 尝试找到连续的周次序列
            if (index + 1 < sorted.size) {
                val nextWeek = sorted[index + 1]
                // 检查是否连续（步长为1或2，即单周/双周）
                if (nextWeek == startWeek + 1 || nextWeek == startWeek + 2) {
                    step = nextWeek - startWeek
                    endWeek = nextWeek
                    count = 2
                    var nextIndex = index + 2
                    // 继续查找相同步长的连续周次
                    while (nextIndex < sorted.size && sorted[nextIndex] == endWeek + step) {
                        endWeek = sorted[nextIndex]
                        count++
                        nextIndex++
                    }

                    // 如果是单周/双周且至少3个周次，添加特殊标记
                    if (step == 2 && count >= 3) {
                        val type = if (startWeek % 2 == 0) "(双周)" else "(单周)"
                        result.add("${startWeek}-${endWeek}周$type")
                        index = nextIndex
                        continue
                    } else if (step == 1) {
                        // 连续周次，合并显示
                        result.add("${startWeek}-${endWeek}周")
                        index = nextIndex
                        continue
                    }
                }
            }

            // 单独一个周次
            if (startWeek == endWeek) {
                result.add("${startWeek}周")
            } else {
                // 不应该走到这里，但作为fallback
                result.add("${startWeek}周")
            }
            index++
        }

        return result.joinToString("、")
    }
}
