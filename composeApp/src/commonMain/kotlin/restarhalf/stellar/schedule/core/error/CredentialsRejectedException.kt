package restarhalf.stellar.schedule.core.error

/**
 * 登录接口明确拒绝了账号密码（账号不存在或密码错误）。
 *
 * 与网络、超时、服务端 5xx 区分开：这类失败重试没有意义，应当直接退出登录。
 */
class CredentialsRejectedException(message: String) : Exception(message)

private val CREDENTIAL_REJECTION_HINTS = listOf(
    "密码错误",
    "帐号不存在",
    "账号不存在",
    "用户不存在",
)

/**
 * 判断登录失败提示是否属于"账号或密码错误"。
 *
 * 教务系统：该帐号不存在或密码错误！
 * 体测系统：用户名或者密码错误
 */
fun isCredentialRejection(message: String): Boolean =
    message.isNotBlank() && CREDENTIAL_REJECTION_HINTS.any(message::contains)
