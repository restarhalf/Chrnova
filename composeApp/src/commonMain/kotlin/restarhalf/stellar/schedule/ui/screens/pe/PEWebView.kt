package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 平台 WebView，加载体育系统官方页并注入免登录会话。
 *
 * @param targetUrl 最终要展示的页面（含 hash 路由）
 * @param authScript 注入脚本（写入 tokenMobile / userid / is_passwd_upd）
 * @param modifier 布局修饰符
 */
@Composable
expect fun PEWebView(
    targetUrl: String,
    authScript: String,
    modifier: Modifier = Modifier,
)
