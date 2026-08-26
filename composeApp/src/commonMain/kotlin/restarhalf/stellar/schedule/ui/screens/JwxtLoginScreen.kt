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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Eye
import restarhalf.stellar.schedule.ui.icons.EyeOff
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.JwxtLoginViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 教务系统登录页面
 *
 * @param vm JW登录ViewModel
 * @param onBack 返回回调
 * @param onLoginSuccess 登录成功回调
 */
@Composable
fun JwxtLoginScreen(
    vm: JwxtLoginViewModel,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    inWel: Boolean,
    next: () -> Unit
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    fun submitFromIme() {
        if (uiState.loading ||
            uiState.userNo.isBlank() ||
            uiState.password.isBlank()
        ) {
            return
        }
        focusManager.clearFocus()
        vm.submitLogin(onSuccess = onLoginSuccess)
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "登录教务系统账号",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    if (!inWel) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Back,
                                contentDescription = "返回",
                                tint = colors.onBackground
                            )
                        }
                    }

                },
            )
        },
        bottomBar = {
            Column {
                if (inWel) {
                    Button(
                        onClick = { next() },
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text(
                            text = "跳过",
                            color = colors.onBackground
                        )
                    }
                }
                Button(
                    enabled = !uiState.loading &&
                            uiState.userNo.isNotBlank() &&
                            uiState.password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp).imePadding(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = {
                        focusManager.clearFocus()
                        vm.submitLogin(onSuccess = onLoginSuccess)
                    }
                ) {
                    Text(
                        text = if (uiState.loading) "登录中..." else "登录",
                        color = colors.onPrimary
                    )
                }
            }
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
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
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
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentType = ContentType.Username
                        }
                    )
                    TextField(
                        label = "密码",
                        value = uiState.password,
                        onValueChange = { vm.onPasswordChange(it) },
                        singleLine = true,
                        visualTransformation =
                            if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitFromIme() }),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) EyeOff else Eye,
                                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                    tint = colors.onSurfaceVariantSummary,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentType = ContentType.Password
                        }
                    )
                    if (uiState.password.contains(Regex("[^\u0000-\u00ff]"))) {
                        Text(
                            text = "密码包含中文字符了，确定吗",
                            color = colors.error,
                            style = MiuixTheme.textStyles.footnote1,
                        )
                    }
                    Text(
                        text = uiState.error,
                        color = colors.error,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            }
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "此为教务系统账号登录",
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "账号是你的学号，密码是信息门户的密码\n如：\n学号：2021081125\n密码：your_password\n登录后可获取课程和考务等",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}
