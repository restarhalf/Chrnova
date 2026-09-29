package restarhalf.stellar.schedule.ui.screens.pe

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PEWebAuthTest {

    @Test
    fun `官方预约页指向体测预约hash路由`() {
        assertTrue(PEWebAuth.APPOINTMENT_URL.startsWith("http://39.100.89.70/mobile/"))
        assertContains(PEWebAuth.APPOINTMENT_URL, "gym-appoint")
    }

    @Test
    fun `注入脚本写入tokenMobile与userid`() {
        val script = PEWebAuth.buildInjectScript(token = "tok-1", userId = "uid-2")
        assertContains(script, "localStorage.setItem('tokenMobile'")
        assertContains(script, "localStorage.setItem('userid'")
        assertContains(script, "localStorage.setItem('is_passwd_upd'")
        assertContains(script, "tok-1")
        assertContains(script, "uid-2")
    }

    @Test
    fun `注入脚本转义单引号与script结束标签`() {
        val script = PEWebAuth.buildInjectScript(
            token = "a'b</script>",
            userId = "u\\n",
        )
        assertFalse(script.contains("a'b"))
        assertContains(script, "a\\'b")
        assertContains(script, "\\u003C")
        assertContains(script, "\\u003E")
        assertContains(script, "u\\\\n")
    }

    @Test
    fun `HTML注入把脚本插在head之后`() {
        val html = "<!DOCTYPE html><html><head><meta charset=utf-8></head><body></body></html>"
        val patched = PEWebAuth.injectScriptIntoHtml(html, "window.__pe=1;")
        assertTrue(patched.indexOf("window.__pe=1;") > patched.indexOf("<head>"))
        assertTrue(patched.indexOf("window.__pe=1;") < patched.indexOf("</head>"))
    }

    @Test
    fun `预约页URL判定排除login`() {
        assertTrue(PEWebAuth.isAppointmentUrl("http://39.100.89.70/mobile/#/gym-appoint?title=x"))
        assertFalse(PEWebAuth.isAppointmentUrl("http://39.100.89.70/mobile/#/login"))
        assertFalse(PEWebAuth.isAppointmentUrl("http://39.100.89.70/mobile/"))
        assertFalse(PEWebAuth.isAppointmentUrl(null))
    }
}
