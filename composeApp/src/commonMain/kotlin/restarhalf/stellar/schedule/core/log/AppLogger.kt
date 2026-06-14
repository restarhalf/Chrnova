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
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        appendToFile(entry)
    }

    fun log(tag: String, message: String, throwable: Throwable, level: Level = Level.ERROR) {
        if (!enabled) return
        val stackTrace = throwable.stackTraceToString()
        val fullMessage = buildString {
            append(message)
            append("\n")
            append(throwable.toString())
            if (stackTrace.isNotBlank()) {
                append("\n")
                append(stackTrace)
            }
        }
        val entry = createEntry(tag, fullMessage, level)
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        appendToFile(entry)
    }

    fun logFatal(threadName: String, throwable: Throwable) {
        val stackTrace = throwable.stackTraceToString()
        val fullMessage = buildString {
            append("Uncaught exception on $threadName")
            append("\n")
            append(throwable.toString())
            if (stackTrace.isNotBlank()) {
                append("\n")
                append(stackTrace)
            }
        }
        val entry = createEntry("FATAL", fullMessage, Level.ERROR)
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        appendToFile(entry)
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
            val line = "[${entry.timestamp}] [${entry.level.tag}/${entry.tag}] ${entry.message}"
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
            val parsed = lines.mapNotNull { line ->
                parseLogLine(line)
            }
            _entries.value = parsed.takeLast(MAX_ENTRIES)
        }
    }

    private fun parseLogLine(line: String): LogEntry? {
        val timestampRegex = Regex("""\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\]""")
        val levelTagRegex = Regex("""\[(\w)/(\w+)\]""")
        val tsMatch = timestampRegex.find(line) ?: return null
        val ltMatch = levelTagRegex.find(line) ?: return null
        val timestamp = tsMatch.groupValues[1]
        val levelTag = ltMatch.groupValues[1]
        val tag = ltMatch.groupValues[2]
        val message = line.substring(ltMatch.range.last + 2)
        val level = Level.entries.find { it.tag == levelTag } ?: Level.INFO
        return LogEntry(timestamp = timestamp, tag = tag, level = level, message = message)
    }

    fun clear() {
        _entries.value = emptyList()
        if (initialized) {
            runCatching { LogFileStorage.clear() }
        }
    }

    fun toPlainText(): String {
        return _entries.value.joinToString("\n") { entry ->
            "[${entry.timestamp}] [${entry.level.tag}/${entry.tag}] ${entry.message}"
        }
    }

    data class LogEntry(
        val timestamp: String,
        val tag: String,
        val level: Level = Level.INFO,
        val message: String,
    )
}
