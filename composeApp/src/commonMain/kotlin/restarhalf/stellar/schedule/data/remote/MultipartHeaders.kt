package restarhalf.stellar.schedule.data.remote

fun contentDispositionFileName(fileName: String): String {
    val cleaned = fileName
        .replace("\r", "")
        .replace("\n", "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "filename=\"$cleaned\""
}
