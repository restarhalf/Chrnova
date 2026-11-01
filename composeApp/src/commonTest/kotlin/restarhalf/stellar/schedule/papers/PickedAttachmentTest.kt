package restarhalf.stellar.schedule.papers

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PickedAttachmentTest {

    private fun attachment(name: String, mime: String = "") =
        PickedAttachment(bytes = ByteArray(0), name = name, mime = mime)

    @Test
    fun `试卷支持pdf和word`() {
        assertTrue(isPaperDocument(attachment("试卷.pdf", "application/pdf")))
        assertTrue(isPaperDocument(attachment("试卷.doc", "application/msword")))
        assertTrue(
            isPaperDocument(
                attachment(
                    "试卷.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                )
            )
        )
    }

    @Test
    fun `扩展名大写也能识别`() {
        assertTrue(isPaperDocument(attachment("试卷.PDF", "application/octet-stream")))
        assertTrue(isPaperDocument(attachment("试卷.DocX", "")))
    }

    @Test
    fun `图片和未知类型被拒绝`() {
        assertFalse(isPaperDocument(attachment("照片.jpg", "image/jpeg")))
        assertFalse(isPaperDocument(attachment("照片.png", "image/png")))
        assertFalse(isPaperDocument(attachment("压缩包.zip", "application/zip")))
        assertFalse(isPaperDocument(attachment("没有扩展名", "")))
    }

    @Test
    fun `扩展名缺失时按MIME判断`() {
        assertTrue(isPaperDocument(attachment("下载文件", "application/pdf")))
        assertFalse(isPaperDocument(attachment("下载文件", "image/png")))
    }
}
