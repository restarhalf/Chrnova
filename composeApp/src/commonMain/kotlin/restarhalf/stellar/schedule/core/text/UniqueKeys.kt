package restarhalf.stellar.schedule.core.text

private const val OCCURRENCE_SEPARATOR = '\u0000'

fun uniqueKey(base: String, occurrence: Int): String =
    if (occurrence <= 1) base else "$base$OCCURRENCE_SEPARATOR$occurrence"

fun <T> List<T>.uniqueKeys(keyOf: (T) -> String): List<String> {
    if (isEmpty()) return emptyList()
    val bases = map(keyOf)
    val reserved = bases.toHashSet()
    val seen = HashMap<String, Int>(bases.size)
    val result = ArrayList<String>(bases.size)
    for (base in bases) {

        val occurrence = (seen[base] ?: 0) + 1
        seen[base] = occurrence
        if (occurrence == 1) {
            result += base
            continue
        }
        var bump = occurrence
        var candidate = uniqueKey(base, bump)
        while (candidate in reserved) {
            bump++
            candidate = uniqueKey(base, bump)
        }
        reserved += candidate
        result += candidate
    }
    return result
}
