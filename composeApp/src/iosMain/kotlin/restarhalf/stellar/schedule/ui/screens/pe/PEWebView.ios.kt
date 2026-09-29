package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun PEWebView(
    targetUrl: String,
    authScript: String,
    modifier: Modifier,
) {
    // 每条 WebView 实例自己的重试计数，避免第一次落在登录页后卡住
    val navRetryHolder = remember { intArrayOf(0) }

    val navigationDelegate = remember(targetUrl, authScript) {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(
                webView: WKWebView,
                decidePolicyForNavigationAction: WKNavigationAction,
                decisionHandler: (WKNavigationActionPolicy) -> Unit,
            ) {
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            }

            override fun webView(
                webView: WKWebView,
                didFinishNavigation: WKNavigation?,
            ) {
                val current = webView.URL?.absoluteString.orEmpty()
                if (PEWebAuth.isAppointmentUrl(current)) {
                    navRetryHolder[0] = 0
                    return
                }
                if (current.startsWith("http://39.100.89.70") && navRetryHolder[0] < 3) {
                    navRetryHolder[0] += 1
                    // DocumentStart 脚本已写入会话；此处整页进预约页
                    NSURL.URLWithString(targetUrl)?.let { target ->
                        webView.loadRequest(NSURLRequest(uRL = target))
                    }
                }
            }
        }
    }

    UIKitView(
        factory = {
            val configuration = WKWebViewConfiguration().apply {
                userContentController.addUserScript(
                    WKUserScript(
                        source = authScript,
                        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                        forMainFrameOnly = true,
                    ),
                )
                defaultWebpagePreferences.allowsContentJavaScript = true
            }
            WKWebView(
                frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, 1.0, 1.0),
                configuration = configuration,
            ).apply {
                this.navigationDelegate = navigationDelegate
                val url = NSURL.URLWithString(targetUrl)!!
                loadRequest(NSURLRequest(uRL = url))
            }
        },
        onRelease = { webView ->
            webView.navigationDelegate = null
            webView.stopLoading()
        },
        modifier = modifier,
    )
}
