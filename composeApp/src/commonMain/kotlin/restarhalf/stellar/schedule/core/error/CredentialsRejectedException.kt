package restarhalf.stellar.schedule.core.error

class CredentialsRejectedException(message: String) : Exception(message)

private val CREDENTIAL_REJECTION_HINTS = listOf(
    "密码错误",
    "帐号不存在",
    "账号不存在",
    "用户不存在",
)

fun isCredentialRejection(message: String): Boolean =
    message.isNotBlank() && CREDENTIAL_REJECTION_HINTS.any(message::contains)
