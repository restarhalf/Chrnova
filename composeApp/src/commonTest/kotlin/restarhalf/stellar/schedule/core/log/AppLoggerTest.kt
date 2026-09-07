package restarhalf.stellar.schedule.core.log

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AppLogger 内存路径测试（不触碰文件存储，不调用 init）。
 * AppLogger 是进程级单例，每个用例先 clear() 保证隔离。
 */
class AppLoggerTest {

    private fun reset() {
        AppLogger.setEnabled(false)
        AppLogger.clear()
    }

    @Test
    fun disabledByDefaultLogsNothing() {
        reset()
        AppLogger.log("Tag", "should be ignored")
        assertTrue(AppLogger.entries.value.isEmpty())
    }

    @Test
    fun enabledLogRecordsEntry() {
        reset()
        AppLogger.setEnabled(true)
        AppLogger.log("Network", "token refreshed", AppLogger.Level.DEBUG)
        val entries = AppLogger.entries.value
        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("Network", entry.tag)
        assertEquals(AppLogger.Level.DEBUG, entry.level)
        assertEquals("token refreshed", entry.message)
        // 时间戳格式 yyyy-MM-dd HH:mm:ss.SSS
        assertTrue(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}""").matches(entry.timestamp))
    }

    @Test
    fun throwableVariantAppendsStacktrace() {
        reset()
        AppLogger.setEnabled(true)
        AppLogger.log("Sync", "boom", RuntimeException("kaboom"), AppLogger.Level.WARN)
        val entry = AppLogger.entries.value.single()
        assertEquals(AppLogger.Level.WARN, entry.level)
        assertTrue(entry.message.startsWith("boom\n"))
        assertTrue("kaboom" in entry.message)
        assertTrue("at " in entry.message || entry.message.isNotBlank())
    }

    @Test
    fun defaultLevelIsInfo() {
        reset()
        AppLogger.setEnabled(true)
        AppLogger.log("App", "hello")
        assertEquals(AppLogger.Level.INFO, AppLogger.entries.value.single().level)
    }

    @Test
    fun levelFromTag() {
        assertEquals(AppLogger.Level.DEBUG, AppLogger.Level.fromTag("D"))
        assertEquals(AppLogger.Level.DEBUG, AppLogger.Level.fromTag("debug"))
        assertEquals(AppLogger.Level.INFO, AppLogger.Level.fromTag("I"))
        assertEquals(AppLogger.Level.WARN, AppLogger.Level.fromTag("WARNING"))
        assertEquals(AppLogger.Level.WARN, AppLogger.Level.fromTag("warning"))
        assertEquals(AppLogger.Level.ERROR, AppLogger.Level.fromTag("E"))
        assertEquals(AppLogger.Level.INFO, AppLogger.Level.fromTag("unknown-tag"))
    }

    @Test
    fun levelTags() {
        assertEquals("DEBUG", AppLogger.Level.DEBUG.tag)
        assertEquals("INFO", AppLogger.Level.INFO.tag)
        assertEquals("WARNING", AppLogger.Level.WARN.tag)
        assertEquals("ERROR", AppLogger.Level.ERROR.tag)
    }

    @Test
    fun toPlainTextEscapesNewlines() {
        reset()
        AppLogger.setEnabled(true)
        AppLogger.log("Multi", "line1\nline2\\path")
        val text = AppLogger.toPlainText()
        // 消息中的换行被转义为 \n 字面量，反斜杠被转义为 \\，单行输出
        assertEquals(1, text.split("\n").size)
        assertTrue("""line1\nline2\\path""" in text)
    }

    @Test
    fun toExportTextContainsHeaderAndMetadata() {
        reset()
        AppLogger.setEnabled(true)
        AppLogger.log("App", "entry-1")
        AppLogger.log("App", "entry-2")
        val exported = AppLogger.toExportText(
            mapOf("version" to "1.2.3", "model" to "Pixel 8"),
        )
        assertTrue(exported.startsWith("=== Chrnova Log Export ==="))
        assertTrue("version: 1.2.3" in exported)
        assertTrue("model: Pixel 8" in exported)
        assertTrue("Log entries: 2" in exported)
        assertTrue("entry-1" in exported)
        assertTrue("entry-2" in exported)
    }

    @Test
    fun clearEmptiesBuffer() {
        reset()
        AppLogger.setEnabled(true)
        AppLogger.log("App", "bye")
        assertEquals(1, AppLogger.entries.value.size)
        AppLogger.clear()
        assertTrue(AppLogger.entries.value.isEmpty())
        assertEquals("", AppLogger.toPlainText())
    }

    @Test
    fun entriesKeepChronologicalIds() {
        reset()
        AppLogger.setEnabled(true)
        AppLogger.log("App", "first")
        AppLogger.log("App", "second")
        val entries = AppLogger.entries.value
        assertEquals(2, entries.size)
        assertTrue(entries[1].id > entries[0].id)
    }
}
