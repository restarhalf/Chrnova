package restarhalf.stellar.schedule.data.remote

/**
 * 体育系统令牌过期异常
 * 
 * 当体育系统登录令牌过期时抛出，用于触发重新登录流程。
 * 
 * @param message 异常消息
 */
class PETokenExpiredException(message: String = "登录已过期，请刷新重试") : Exception(message)
