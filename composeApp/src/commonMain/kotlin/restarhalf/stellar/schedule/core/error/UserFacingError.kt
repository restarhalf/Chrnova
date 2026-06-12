package restarhalf.stellar.schedule.core.error

import kotlinx.coroutines.CancellationException

enum class UserFacingErrorKind(
    val fallbackMessage: String,
    val invalidDataMessage: String = fallbackMessage,
) {
    Login(
        fallbackMessage = "登录失败，请稍后重试",
    ),
    Sync(
        fallbackMessage = "同步失败，请稍后重试",
    ),
    LoadGrades(
        fallbackMessage = "加载成绩失败，请稍后重试",
        invalidDataMessage = "成绩数据暂时无法解析，请稍后重试",
    ),
    LoadExaminations(
        fallbackMessage = "加载考试安排失败，请稍后重试",
        invalidDataMessage = "考试数据暂时无法解析，请稍后重试",
    ),
    CheckUpdate(
        fallbackMessage = "检查更新失败，请稍后重试",
        invalidDataMessage = "更新信息暂时无法解析，请稍后重试",
    ),
    DownloadUpdate(
        fallbackMessage = "下载更新失败，请稍后重试",
    ),
    LoadPEScores(
        fallbackMessage = "加载体测成绩失败，请稍后重试",
        invalidDataMessage = "体测数据暂时无法解析，请稍后重试",
    ),
    LoadPEDetail(
        fallbackMessage = "加载体测详情失败，请稍后重试",
        invalidDataMessage = "体测详情暂时无法解析，请稍后重试",
    ),
    ;
}

fun Throwable.toUserFacingMessage(kind: UserFacingErrorKind): String {
    extractBusinessMessageOrNull()?.let { return it }

    val hints = buildHintText()
    if (this is CancellationException || hints.contains("cancellation")) {
        return "操作已取消"
    }

    if (kind.usesLoginState() && isLoginStateHint(hints)) {
        return "登录状态已失效，请重新登录"
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
        if (message.isNotBlank() && isBusinessFriendly(message)) {
            return message
        }
        current = current?.cause
    }
    return null
}

private fun isBusinessFriendly(message: String): Boolean {
    if (message.length > 40) return false
    val lower = message.lowercase()
    if (STACK_TRACE_HINTS.any(lower::contains)) return false
    return BUSINESS_MESSAGE_HINTS.any(message::contains)
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

private val BUSINESS_MESSAGE_HINTS =
    listOf(
        "请先登录",
        "重新登录",
        "账号",
        "帐号",
        "用户名",
        "密码",
        "验证码",
        "登录",
    )

private val STACK_TRACE_HINTS =
    listOf(
        "exception",
        " stack",
        "trace",
        "\tat ",
        "java.",
        "kotlin.",
        "io.ktor",
        "nsurl",
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
