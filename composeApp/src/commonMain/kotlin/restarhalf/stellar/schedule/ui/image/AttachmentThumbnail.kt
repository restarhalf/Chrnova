package restarhalf.stellar.schedule.ui.image

fun isImageAttachment(mimeType: String, fileName: String): Boolean {
    if (mimeType.startsWith("image/", ignoreCase = true)) return true
    val lower = fileName.lowercase()
    return lower.endsWith(".jpg") ||
        lower.endsWith(".jpeg") ||
        lower.endsWith(".png") ||
        lower.endsWith(".webp") ||
        lower.endsWith(".gif") ||
        lower.endsWith(".bmp") ||
        lower.endsWith(".heic") ||
        lower.endsWith(".heif")
}

fun attachmentBadgeLabel(name: String, mimeType: String): String {
    val ext = name.substringAfterLast('.', missingDelimiterValue = "").uppercase()
    if (ext.isNotEmpty() && ext.length <= 4) return ext
    return when {
        mimeType.contains("pdf", ignoreCase = true) -> "PDF"
        mimeType.contains("msword", ignoreCase = true) -> "DOC"
        mimeType.contains("wordprocessingml", ignoreCase = true) -> "DOCX"
        else -> "FILE"
    }
}
