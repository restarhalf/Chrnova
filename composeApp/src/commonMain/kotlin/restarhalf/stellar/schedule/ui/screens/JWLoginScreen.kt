package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.JWLoginViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect

/**
 * 教务系统登录页面
 *
 * @param vm JW登录ViewModel
 * @param onBack 返回回调
 * @param onLoginSuccess 登录成功回调
 */
@Composable
fun JWLoginScreen(
    vm: JWLoginViewModel,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val overscrollEffect = MiuixOverscrollEffect()
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(title = "登录教务系统账号", scrollBehavior = topAppBarScrollBehavior)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = 0.dp
                    )
                )
                .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = PaddingValues(),
                outerPadding = appScaffoldPadding,
                extraTop = 12.dp,
                extraStart = 16.dp,
                extraEnd = 16.dp,
            ),
            overscrollEffect = overscrollEffect
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        label = "账号",
                        value = uiState.userNo,
                        onValueChange = { vm.onUserNoChange(it) },
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentType = ContentType.Username
                        }
                    )
                    TextField(
                        label = "密码",
                        value = uiState.password,
                        onValueChange = { vm.onPasswordChange(it) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentType = ContentType.Password
                        }
                    )
                    if (uiState.error.isNotBlank()) {
                        Text(
                            text = uiState.error,
                            color = MiuixTheme.colorScheme.error,
                        )
                    }
                    Button(
                        enabled = !uiState.loading &&
                                uiState.userNo.isNotBlank() &&
                                uiState.password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = {
                            vm.submitLogin(onSuccess = onLoginSuccess)
                        }
                    ) {
                        Text(
                            text = if (uiState.loading) "登录中..." else "登录",
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
