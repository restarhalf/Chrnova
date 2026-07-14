package restarhalf.stellar.schedule.core.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object AppLogger {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()
    private var enabled = false
    private var initialized = false
    private const val MAX_ENTRIES = 2000
    private const val MAX_LOG_FILE_SIZE = 512 * 1024L
    private const val ROTATE_KEEP_LINES = 500

    private val buffer = ArrayList<LogEntry>(MAX_ENTRIES + 1)

    enum class Level(val tag: String) {
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E"),
    }

    fun init(logDir: String = "") {
        if (initialized) return
        initialized = true
        runCatching {
            LogFileStorage.init(logDir)
            loadLogsFromFile()
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun log(tag: String, message: String, level: Level = Level.INFO) {
        if (!enabled) return
        val entry = createEntry(tag, message, level)
        appendEntry(entry)
        appendToFile(entry)
    }

    fun log(tag: String, message: String, throwable: Throwable, level: Level = Level.ERROR) {
        if (!enabled) return
        val fullMessage = buildString {
            append(message)
            append("\n")
            append(throwable.toString())
            val stackTrace = throwable.stackTraceToString()
            if (stackTrace.isNotBlank()) {
                append("\n")
                append(stackTrace)
            }
        }
        val entry = createEntry(tag, fullMessage, level)
        appendEntry(entry)
        appendToFile(entry)
    }

    fun logFatal(threadName: String, throwable: Throwable) {
        if (!initialized) {
            runCatching { LogFileStorage.init("") }
            initialized = true
        }
        val fullMessage = buildString {
            append("Uncaught exception on $threadName")
            append("\n")
            append(throwable.toString())
            val stackTrace = throwable.stackTraceToString()
            if (stackTrace.isNotBlank()) {
                append("\n")
                append(stackTrace)
            }
        }
        val entry = createEntry("FATAL", fullMessage, Level.ERROR)
        appendEntry(entry)
        appendToFile(entry)
        runCatching { LogFileStorage.sync() }
    }

    private fun appendEntry(entry: LogEntry) {
        buffer.add(entry)
        if (buffer.size > MAX_ENTRIES) {
            buffer.subList(0, buffer.size - MAX_ENTRIES).clear()
        }
        _entries.value = buffer.toList()
    }

    private fun createEntry(tag: String, message: String, level: Level): LogEntry {
        val now = Clock.System.now()
        val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val timestamp = "${local.date} ${local.time.hour.toString().padStart(2, '0')}:${local.time.minute.toString().padStart(2, '0')}:${local.time.second.toString().padStart(2, '0')}.${local.time.nanosecond.toString().take(3)}"
        return LogEntry(timestamp = timestamp, tag = tag, level = level, message = message)
    }

    private fun appendToFile(entry: LogEntry) {
        if (!initialized) return
        runCatching {
            val escapedMessage = entry.message.replace("\\", "\\\\").replace("\n", "\\n")
            val line = "[${entry.timestamp}] [${entry.level.tag}/${entry.tag}] $escapedMessage"
            LogFileStorage.appendLine(line)
            checkAndRotate()
        }
    }

    private fun checkAndRotate() {
        runCatching {
            if (LogFileStorage.getFileSize() <= MAX_LOG_FILE_SIZE) return
            val lines = LogFileStorage.readAllLines()
            if (lines.size > ROTATE_KEEP_LINES) {
                LogFileStorage.rewriteLines(lines.takeLast(ROTATE_KEEP_LINES))
            }
        }
    }

    private fun loadLogsFromFile() {
        runCatching {
            val lines = LogFileStorage.readAllLines()
            if (lines.isEmpty()) return
            val timestampPrefixRegex = Regex("""^\[\d{4}-\d{2}-\d{2}""")
            val joined = mutableListOf<String>()
            for (line in lines) {
                if (line.isNotEmpty() && timestampPrefixRegex.containsMatchIn(line)) {
                    joined.add(line)
                } else if (joined.isNotEmpty()) {
                    joined[joined.lastIndex] += "\n$line"
                }
            }
            val parsed = joined.mapNotNull { parseLogLine(it) }
            buffer.clear()
            buffer.addAll(parsed.takeLast(MAX_ENTRIES))
            _entries.value = buffer.toList()
        }
    }

    private fun parseLogLine(line: String): LogEntry? {
        val timestampRegex = Regex("""\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\]""")
        val levelTagRegex = Regex("""\[(\w+)/([\w.]+)\]""")
        val tsMatch = timestampRegex.find(line) ?: return null
        val ltMatch = levelTagRegex.find(line) ?: return null
        val timestamp = tsMatch.groupValues[1]
        val levelTag = ltMatch.groupValues[1]
        val tag = ltMatch.groupValues[2]
        val rawMessage = line.substring(ltMatch.range.last + 2)
        val message = rawMessage.replace("\\n", "\n").replace("\\\\", "\\")
        val level = Level.entries.find { it.tag == levelTag } ?: Level.INFO
        return LogEntry(timestamp = timestamp, tag = tag, level = level, message = message)
    }

    fun clear() {
        buffer.clear()
        _entries.value = emptyList()
        if (initialized) {
            runCatching { LogFileStorage.clear() }
        }
    }

    fun toPlainText(): String {
        return buffer.joinToString("\n") { entry ->
            val escapedMessage = entry.message.replace("\\", "\\\\").replace("\n", "\\n")
            "[${entry.timestamp}] [${entry.level.tag}/${entry.tag}] $escapedMessage"
        }
    }

    /**
     * 导出日志供 bug 反馈，包含设备元数据和可读堆栈。
     */
    fun toExportText(metadata: Map<String, String> = emptyMap()): String {
        val header = buildString {
            appendLine("=== Chrnova Log Export ===")
            for ((key, value) in metadata) {
                appendLine("$key: $value")
            }
            appendLine("Log entries: ${buffer.size}")
            appendLine("==========================")
        }
        val body = buffer.joinToString("\n") { entry ->
            "[${entry.timestamp}] [${entry.level.tag}/${entry.tag}] ${entry.message}"
        }
        return header + body
    }

    data class LogEntry(
        val id: Long = nextId(),
        val timestamp: String,
        val tag: String,
        val level: Level = Level.INFO,
        val message: String,
    ) {
        companion object {
            private var counter = 0L
            private fun nextId() = counter++
        }
    }
}
