package restarhalf.stellar.schedule.ui.screens.pe

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.net.HttpURLConnection
import java.net.URL

private const val PE_HOST = "39.100.89.70"
private const val PE_ORIGIN_PREFIX = "http://$PE_HOST"
private const val MAX_NAV_RETRY = 3

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PEWebView(
    targetUrl: String,
    authScript: String,
    modifier: Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            var navRetry = 0
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.userAgentString = settings.userAgentString
                    ?.replace("; wv", "")
                    ?: WebSettings.getDefaultUserAgent(context)

                webViewClient = object : WebViewClient() {
                    /**
                     * 在 HTML 文档返回给解析器之前把会话脚本插进 `<head>`，
                     * 保证先于 Vue 路由守卫执行，避免「第一次进登录页」。
                     */
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val req = request ?: return null
                        if (!req.method.equals("GET", ignoreCase = true)) return null
                        val uri = req.url ?: return null
                        if (!uri.host.equals(PE_HOST, ignoreCase = true)) return null
                        val path = uri.path ?: return null
                        if (path.contains("static")) return null
                        val isMobileDoc = path == "/" || path == "/mobile" || path == "/mobile/"
                        if (!isMobileDoc) return null

                        return runCatching {
                            val connection = (URL(req.url.toString()).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 15_000
                                readTimeout = 15_000
                                requestMethod = "GET"
                                instanceFollowRedirects = true
                            }
                            val code = connection.responseCode
                            if (code !in 200..299) {
                                connection.disconnect()
                                return@runCatching null
                            }
                            val raw = connection.inputStream.use { it.readBytes() }
                            connection.disconnect()
                            val charset = Charsets.UTF_8
                            val html = raw.toString(charset)
                            val patched = PEWebAuth.injectScriptIntoHtml(html, authScript)
                            WebResourceResponse(
                                "text/html",
                                charset.name(),
                                200,
                                "OK",
                                mapOf("Content-Type" to "text/html; charset=utf-8"),
                                patched.byteInputStream(charset),
                            )
                        }.getOrNull()
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        // 双保险：即便拦截没命中，也在文档开始时再写一次
                        if (url != null && url.startsWith(PE_ORIGIN_PREFIX)) {
                            view?.evaluateJavascript(authScript, null)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (view == null || url.isNullOrBlank()) return
                        if (!url.startsWith(PE_ORIGIN_PREFIX)) return
                        if (PEWebAuth.isAppointmentUrl(url)) {
                            navRetry = 0
                            return
                        }
                        // 落在登录页 / 首页时：再注一次会话，然后整页进预约页
                        if (navRetry < MAX_NAV_RETRY) {
                            navRetry++
                            view.evaluateJavascript(authScript) {
                                view.loadUrl(targetUrl)
                            }
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean = false
                }

                loadUrl(targetUrl)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
    )
}
