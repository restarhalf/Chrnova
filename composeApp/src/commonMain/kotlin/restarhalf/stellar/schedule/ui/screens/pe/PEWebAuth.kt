package restarhalf.stellar.schedule.ui.screens.pe

object PEWebAuth {
    const val APPOINTMENT_URL: String =
        "http://39.100.89.70/mobile/#/gym-appoint?title=%E4%BD%93%E6%B5%8B%E9%A2%84%E7%BA%A6"

    const val MOBILE_BASE_URL: String = "http://39.100.89.70/mobile/"

    const val APPOINTMENT_HASH_MARK: String = "gym-appoint"

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

    fun isAppointmentUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains(APPOINTMENT_HASH_MARK) && !url.contains("login")
    }

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
