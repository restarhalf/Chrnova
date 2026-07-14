package restarhalf.stellar.schedule.core.error

import kotlinx.coroutines.CancellationException

/**
 * 用户可见错误类型枚举
 * 
 * 定义应用中可能出现的错误类型，每种类型包含默认错误消息和数据格式错误消息。
 */
enum class UserFacingErrorKind(
    /** 默认错误消息，用于通用错误情况 */
    val fallbackMessage: String,
    /** 数据格式错误消息，用于服务器返回数据无法解析的情况 */
    val invalidDataMessage: String = fallbackMessage,
) {
    /** 登录失败 */
    Login(
        fallbackMessage = "登录失败，请稍后重试",
    ),
    /** 课程同步失败 */
    Sync(
        fallbackMessage = "同步失败，请稍后重试",
    ),
    /** 成绩加载失败 */
    LoadGrades(
        fallbackMessage = "加载成绩失败，请稍后重试",
        invalidDataMessage = "成绩数据暂时无法解析，请稍后重试",
    ),
    /** 考试安排加载失败 */
    LoadExaminations(
        fallbackMessage = "加载考试安排失败，请稍后重试",
        invalidDataMessage = "考试数据暂时无法解析，请稍后重试",
    ),
    /** 检查更新失败 */
    CheckUpdate(
        fallbackMessage = "检查更新失败，请稍后重试",
        invalidDataMessage = "更新信息暂时无法解析，请稍后重试",
    ),
    /** 下载更新失败 */
    DownloadUpdate(
        fallbackMessage = "下载更新失败，请稍后重试",
    ),
    /** 体测成绩加载失败 */
    LoadPEScores(
        fallbackMessage = "加载体测成绩失败，请稍后重试",
        invalidDataMessage = "体测数据暂时无法解析，请稍后重试",
    ),
    /** 体测详情加载失败 */
    LoadPEDetail(
        fallbackMessage = "加载体测详情失败，请稍后重试",
        invalidDataMessage = "体测详情暂时无法解析，请稍后重试",
    ),
    ;
}

/**
 * 将异常转换为用户友好的错误消息
 * 
 * 该函数会分析异常的类型和消息，提取有意义的业务错误信息，
 * 或者根据错误类型返回相应的默认提示消息。
 * 
 * @param kind 错误类型，用于确定使用哪种默认消息
 * @return 用户友好的错误消息字符串
 */
fun Throwable.toUserFacingMessage(kind: UserFacingErrorKind): String {
    // 尝试提取业务友好的错误消息
    extractBusinessMessageOrNull()?.let { return it }

    val hints = buildHintText()
    // 处理协程取消异常
    if (this is CancellationException || hints.contains("cancellation")) {
        return "操作已取消"
    }

    // 检查登录状态失效
    if (kind.usesLoginState() && isLoginStateHint(hints)) {
        return "登录已过期，请刷新重试"
    }

    return when {
        isTimeoutHint(hints) -> "网络超时，请稍后重试"
        hints.contains("502") || hints.contains("503") || hints.contains("504") ->
            "教务系统暂时不可用 (HTTP 502/503/504)，请稍后重试"
        isNetworkHint(hints) ->
            "网络连接异常，请检查网络后重试"
        isInvalidDataHint(hints) -> kind.invalidDataMessage
        else -> kind.fallbackMessage
    }
}

private fun UserFacingErrorKind.usesLoginState(): Boolean =
    this != UserFacingErrorKind.CheckUpdate && this != UserFacingErrorKind.DownloadUpdate

private fun Throwable.extractBusinessMessageOrNull(): String? {
    var current: Throwable? = this
    repeat(6) {
        val message = current?.message?.normalizeForDisplay().orEmpty()
        if (message.isNotBlank() && isUserFacingMessage(message)) {
            // 如果消息包含登录状态关键词，不作为业务消息返回，交给后续登录状态检查处理
            if (!isLoginStateHint(message.lowercase())) {
                return message
            }
        }
        current = current?.cause
    }
    return null
}

/**
 * 判断消息是否适合作为用户可见的错误提示
 *
 * 只接受短小的中文业务消息，其余一律视为技术性消息。
 */
private fun isUserFacingMessage(message: String): Boolean {
    if (message.length > 60) return false
    val lower = message.lowercase()
    // 排除技术性消息
    if (TECHNICAL_MESSAGE_HINTS.any(lower::contains)) return false
    if (Regex("""http\s*\d{3}""").containsMatchIn(lower)) return false
    if (Regex("""\d{1,3}(\.\d{1,3}){3}""").containsMatchIn(message)) return false
    return true
}

private fun Throwable.buildHintText(): String {
    val parts = mutableListOf<String>()
    var current: Throwable? = this
    repeat(6) {
        val error = current ?: return@repeat
        error::class.simpleName?.let(parts::add)
        error.message?.let(parts::add)
        current = error.cause
    }
    return parts.joinToString(" ").normalizeForDisplay().lowercase()
}

private fun String.normalizeForDisplay(): String =
    replace('\n', ' ')
        .replace('\r', ' ')
        .replace('\t', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

private fun isLoginStateHint(hints: String): Boolean = LOGIN_STATE_HINTS.any(hints::contains)

private fun isTimeoutHint(hints: String): Boolean = TIMEOUT_HINTS.any(hints::contains)

private fun isNetworkHint(hints: String): Boolean = NETWORK_HINTS.any(hints::contains)

private fun isInvalidDataHint(hints: String): Boolean = INVALID_DATA_HINTS.any(hints::contains)

/** 技术性消息关键词 — 命中任一则不作为用户提示展示 */
private val TECHNICAL_MESSAGE_HINTS =
    listOf(
        // 连接/网络
        "fail to connect",
        "failed to connect",
        "connection refused",
        "connection reset",
        "connection timed out",
        "connectexception",
        "socket",
        "sockettimeout",
        "unknownhost",
        "unable to resolve",
        "unresolved address",
        "dns",
        "network is unreachable",
        "network connection was lost",
        "not connected to internet",
        "无法连接",
        // 超时
        "timeout",
        "timed out",
        "request timeout",
        "deadline exceeded",
        // 协议/状态码
        "http",
        "https",
        "ssl",
        "tls",
        "status code",
        "响应码",
        // 堆栈
        "exception",
        " stack",
        "trace",
        "\tat ",
        "java.",
        "kotlin.",
        "io.ktor",
        "nsurl",
        "android.",
        "com.google",
        // 序列化
        "serialization",
        "deserialize",
        "encode",
        "decode",
        "json",
        // 其他
        "null pointer",
        "index out of",
        "out of bounds",
        "class not found",
        "no such method",
    )

private val LOGIN_STATE_HINTS =
    listOf(
        "401",
        "403",
        "unauthorized",
        "forbidden",
        "token",
        "请先登录",
        "重新登录",
        "未授权",
    )

private val TIMEOUT_HINTS =
    listOf(
        "timeout",
        "timed out",
        "sockettimeout",
        "request timeout",
        "deadline exceeded",
        "超时",
    )

private val NETWORK_HINTS =
    listOf(
        "unknownhost",
        "unable to resolve host",
        "network is unreachable",
        "network connection was lost",
        "not connected to internet",
        "connectexception",
        "connection reset",
        "unresolved address",
        "dns",
        "socket",
        "无法连接",
        "网络",
    )

fun Throwable.isNetworkError(): Boolean {
    val hints = buildHintText()
    return isNetworkHint(hints) || isTimeoutHint(hints)
}

private val INVALID_DATA_HINTS =
    listOf(
        "serialization",
        "deserialize",
        "decode",
        "parsing",
        "parse",
        "unexpected",
        "json",
        "response",
        "响应",
        "解析",
        "数据异常",
        "空",
        "empty",
        " id",
    )
