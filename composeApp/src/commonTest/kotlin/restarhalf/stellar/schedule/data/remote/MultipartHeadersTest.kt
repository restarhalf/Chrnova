package restarhalf.stellar.schedule.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class MultipartHeadersTest {

    @Test
    fun `普通文件名原样保留`() {
        assertEquals("filename=\"试卷.pdf\"", contentDispositionFileName("试卷.pdf"))
    }

    @Test
    fun `引号被转义`() {
        assertEquals("filename=\"a\\\"b.pdf\"", contentDispositionFileName("a\"b.pdf"))
    }

    @Test
    fun `反斜杠被转义`() {
        assertEquals("filename=\"a\\\\b.pdf\"", contentDispositionFileName("a\\b.pdf"))
    }

    @Test
    fun `换行被去掉`() {
        assertEquals(
            "filename=\"ab.pdf\"",
            contentDispositionFileName("a\r\nb.pdf"),
        )
    }
}
