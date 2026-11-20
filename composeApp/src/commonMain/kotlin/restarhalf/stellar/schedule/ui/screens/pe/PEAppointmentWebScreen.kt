package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.chrome.AppPageScaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.collapsibleTopInset
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PEAppointmentWebScreen(
    onBack: () -> Unit,
    peAuth: PEAuthPort = koinInject(),
) {
    val token by peAuth.observeToken().collectAsStateWithLifecycle(
        initialValue = peAuth.getToken().orEmpty(),
    )
    val userId = peAuth.getUserId()
    val canAutoLogin = token.isNotBlank() && !userId.isNullOrBlank()

    AppPageScaffold(
        title = "体测预约",
        onBack = onBack,
    ) { scrollBehavior, paddingValues ->
        if (canAutoLogin) {
            PEWebView(
                targetUrl = PEWebAuth.APPOINTMENT_URL,
                authScript = PEWebAuth.buildInjectScript(
                    token = token,
                    userId = userId.orEmpty(),
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .collapsibleTopInset(scrollBehavior)
                    .padding(
                        PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = 0.dp,
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .collapsibleTopInset(scrollBehavior)
                    .padding(24.dp),
            ) {
                AppCard(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "请先登录体测系统，登录后可免登录打开官方预约页。",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
