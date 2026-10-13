package restarhalf.stellar.schedule.core.time


object SemesterUtils {

    
    fun parseSemesterKey(semesterId: String): Triple<Int, Int, Int>? {
        val parts = semesterId.trim().split("-")
        if (parts.size < 3) return null
        val y1 = parts[0].toIntOrNull() ?: return null
        val y2 = parts[1].toIntOrNull() ?: return null
        val t = parts[2].toIntOrNull() ?: return null
        return Triple(y1, y2, t)
    }

    
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
