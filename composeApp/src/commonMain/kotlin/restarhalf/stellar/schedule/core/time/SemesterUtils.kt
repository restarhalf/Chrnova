package restarhalf.stellar.schedule.core.time

/**
 * 学期ID解析与排序工具
 *
 * 支持 "2023-2024-1" 格式的学期ID比较。
 */
object SemesterUtils {

    /**
     * 解析学期ID为可比较的三元组 (起始年, 结束年, 学期序号)
     *
     * @return Triple 或 null（格式不合法时）
     */
    fun parseSemesterKey(semesterId: String): Triple<Int, Int, Int>? {
        val parts = semesterId.trim().split("-")
        if (parts.size < 3) return null
        val y1 = parts[0].toIntOrNull() ?: return null
        val y2 = parts[1].toIntOrNull() ?: return null
        val t = parts[2].toIntOrNull() ?: return null
        return Triple(y1, y2, t)
    }

    /**
     * 学期ID比较器，支持直接用于 [Iterable.sortedWith]
     *
     * 排序规则：起始年 → 结束年 → 学期序号，无法解析的排在末尾。
     */
    val comparator: Comparator<String> = Comparator { a, b ->
        val ka = parseSemesterKey(a)
        val kb = parseSemesterKey(b)
        when {
            ka != null && kb != null -> {
                if (ka.first != kb.first) ka.first.compareTo(kb.first)
                else if (ka.second != kb.second) ka.second.compareTo(kb.second)
                else ka.third.compareTo(kb.third)
            }
            ka != null -> 1
            kb != null -> -1
            else -> a.compareTo(b)
        }
    }
}
