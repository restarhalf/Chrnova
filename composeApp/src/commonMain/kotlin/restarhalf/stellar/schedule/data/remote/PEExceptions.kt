package restarhalf.stellar.schedule.data.remote


class PETokenExpiredException(message: String = "登录已过期，请刷新重试") : Exception(message)
