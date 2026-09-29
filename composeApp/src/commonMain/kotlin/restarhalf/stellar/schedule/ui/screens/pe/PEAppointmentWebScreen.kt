package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import restarhalf.stellar.schedule.data.remote.PEAuthStore
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 官方体测预约网页（免登录）。
 *
 * 学校验收时点开即可看到官方预约页，会话由 App 注入，不再出现登录页。
 */
@Composable
fun PEAppointmentWebScreen(
    onBack: () -> Unit,
    authStore: PEAuthStore = koinInject(),
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val token = authStore.getToken()
    val userId = authStore.getUserId()
    val canAutoLogin = !token.isNullOrBlank() && !userId.isNullOrBlank()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "体测预约",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (canAutoLogin) {
            PEWebView(
                targetUrl = PEWebAuth.APPOINTMENT_URL,
                authScript = PEWebAuth.buildInjectScript(
                    token = token.orEmpty(),
                    userId = userId.orEmpty(),
                ),
                modifier = Modifier
                    .fillMaxSize()
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
