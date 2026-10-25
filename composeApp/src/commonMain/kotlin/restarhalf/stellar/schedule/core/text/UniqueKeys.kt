package restarhalf.stellar.schedule.core.text


private const val OCCURRENCE_SEPARATOR = '\u0000'

/**
 * 为重复的业务 key 追加出现序号，保证 LazyColumn 的 item key 唯一。
 *
 * 首次出现保持原样，因此正常数据下 key 与业务 id 完全一致，不会破坏
 * `animateItem` 的动画连续性。分隔符用 NUL，避免业务 id 自身含 `#` 之类
 * 可见字符时与后缀撞车。
 */
fun uniqueKey(base: String, occurrence: Int): String =
    if (occurrence <= 1) base else "$base$OCCURRENCE_SEPARATOR$occurrence"

/**
 * 按 [keyOf] 为列表逐项生成唯一 key，顺序与接收者一致。
 *
 * 用于 LazyColumn 的 key 兜底：业务数据重复时（例如教务后台同一条记录返回两次）
 * 不会因为 key 撞车而崩溃，而是让每条记录各自独立渲染。
 *
 * 生成的候选 key 会与全部原始 key 比对，被占用就继续递增序号，所以即使业务 id
 * 恰好长得像兜底 key（例如本身就带 NUL 和数字）也不会撞车。
 */
fun <T> List<T>.uniqueKeys(keyOf: (T) -> String): List<String> {
    if (isEmpty()) return emptyList()
    val bases = map(keyOf)
    val reserved = bases.toHashSet()
    val seen = HashMap<String, Int>(bases.size)
    val result = ArrayList<String>(bases.size)
    for (base in bases) {
        // 不用 HashMap.merge：那是 JVM 专有 API，iOS 目标没有
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
