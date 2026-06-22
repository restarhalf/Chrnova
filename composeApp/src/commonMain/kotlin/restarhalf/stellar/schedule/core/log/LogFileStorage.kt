package restarhalf.stellar.schedule.core.log

expect object LogFileStorage {
    fun init(logDir: String = "")
    fun appendLine(line: String)
    fun readAllLines(): List<String>
    fun rewriteLines(lines: List<String>)
    fun getFileSize(): Long
    fun sync()
    fun clear()
}
