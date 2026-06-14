package restarhalf.stellar.schedule.core.log

import java.io.File

actual object LogFileStorage {
    private var logFile: File? = null

    actual fun init(logDir: String) {
        val dir = File(logDir)
        if (!dir.exists()) dir.mkdirs()
        logFile = File(dir, "app.log")
    }

    actual fun appendLine(line: String) {
        logFile?.appendText(line + "\n")
    }

    actual fun readAllLines(): List<String> {
        return logFile?.takeIf { it.exists() }?.readLines() ?: emptyList()
    }

    actual fun rewriteLines(lines: List<String>) {
        logFile?.writeText(lines.joinToString("\n") + "\n")
    }

    actual fun getFileSize(): Long {
        return logFile?.takeIf { it.exists() }?.length() ?: 0L
    }

    actual fun clear() {
        logFile?.delete()
    }
}
