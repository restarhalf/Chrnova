package restarhalf.stellar.schedule.ui.screens.pe

/**
 * 体育系统移动端网页鉴权与入口。
 *
 * 官方 SPA（http://39.100.89.70/mobile/）把会话存在 `localStorage.tokenMobile` 与
 * `localStorage.userid`，路由守卫据此判断是否免登录进入。App 必须在 **文档脚本
 * 执行前** 写入这两项，否则第一次打开会被踢到登录页（localStorage 才写入），
 * 第二次打开才能进预约页。
 */
object PEWebAuth {
    /** 体测预约官方页面（hash 路由 + 标题参数） */
    const val APPOINTMENT_URL: String =
        "http://39.100.89.70/mobile/#/gym-appoint?title=%E4%BD%93%E6%B5%8B%E9%A2%84%E7%BA%A6"

    /** SPA 同源根 */
    const val MOBILE_BASE_URL: String = "http://39.100.89.70/mobile/"

    /** 目标 hash 片段（用于判断是否已进入预约页） */
    const val APPOINTMENT_HASH_MARK: String = "gym-appoint"

    /**
     * 构造写入会话的脚本（必须在文档任意业务 JS 之前执行）。
     *
     * 额外写入 `is_passwd_upd=1`，避免 SPA 把缺省值当成「未改密」跳到改密页。
     *
     * @param token 登录响应 token
     * @param userId 登录响应 user_id
     */
    fun buildInjectScript(token: String, userId: String): String {
        val tokenJs = token.toJsStringLiteral()
        val userIdJs = userId.toJsStringLiteral()
        return """
            (function(){
              try {
                localStorage.setItem('tokenMobile', $tokenJs);
                localStorage.setItem('userid', $userIdJs);
                localStorage.setItem('is_passwd_upd', '1');
              } catch (e) {}
            })();
        """.trimIndent()
    }

    /**
     * 将脚本直接嵌入 HTML 的 `<head>`，保证先于 Vue / 路由守卫执行。
     * 找不到 `<head>` 时插到文档最前。
     */
    fun injectScriptIntoHtml(html: String, authScript: String): String {
        val tag = "<script>$authScript</script>"
        val headIndex = html.indexOf("<head>", ignoreCase = true)
        return if (headIndex >= 0) {
            html.substring(0, headIndex + "<head>".length) +
                tag +
                html.substring(headIndex + "<head>".length)
        } else if (html.startsWith("<!DOCTYPE", ignoreCase = true)) {
            val gt = html.indexOf('>').let { if (it >= 0) it + 1 else 0 }
            html.substring(0, gt) + tag + html.substring(gt)
        } else {
            tag + html
        }
    }

    /** 当前 URL 是否已是预约页（排除误匹配） */
    fun isAppointmentUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains(APPOINTMENT_HASH_MARK) && !url.contains("login")
    }

    /** 将字符串转为可安全嵌入 JS 的单引号字面量 */
    internal fun String.toJsStringLiteral(): String = buildString {
        append('\'')
        this@toJsStringLiteral.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '\'' -> append("\\'")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '<' -> append("\\u003C")
                '>' -> append("\\u003E")
                '&' -> append("\\u0026")
                else -> append(ch)
            }
        }
        append('\'')
    }
}
