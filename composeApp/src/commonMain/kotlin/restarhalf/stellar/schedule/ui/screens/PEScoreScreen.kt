package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.screen.pe.PEQRCode
import restarhalf.stellar.schedule.ui.icons.Logout
import restarhalf.stellar.schedule.ui.icons.QrCode
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect

@Composable
fun PEScoreScreen(
    onNavigateToDetail: (String) -> Unit,
) {
    val viewModel: PEViewModel = koinViewModel()
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val overscrollEffect = MiuixOverscrollEffect()
    val yearScores by viewModel.yearScores.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val studentInfo by viewModel.studentInfo.collectAsState()
    val needsLogin by viewModel.needsLogin.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val showPeQrCode = remember { mutableStateOf(false) }
    val loggedIn = viewModel.isLoggedIn()

    LaunchedEffect(needsLogin) {
        if (needsLogin) {
            showLoginDialog = true
        }
    }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            viewModel.loadScoreList()
            viewModel.loadStudentInfo()
        }
    }

    val scoreScreenStatus by viewModel.scoreScreenStatus.collectAsState()
    val statusText = viewModel.buildScoreStatusText(scoreScreenStatus)
    val colors = MiuixTheme.colorScheme

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "体测",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = { showPeQrCode.value = true }) {
                            Icon(imageVector = QrCode, contentDescription = "二维码")
                        }
                    },
                    actions = {
                        if (loggedIn) {
                            IconButton(onClick = { showLogoutConfirm = true }) {
                                Icon(imageVector = Logout, contentDescription = "退出登录")
                            }
                        }
                    }
                )
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.clip(CircleShape)
                                .background(colors.surfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(fontSize = 12.sp, text = statusText ?: "")
                        }
                    }
                }
            }
        },
        popupHost = {
            if (showPeQrCode.value && studentInfo != null) {
                OverlayDialog(
                    show = showPeQrCode.value,
                    title = "体测二维码",
                    onDismissRequest = { showPeQrCode.value = false }
                ) {
                    Box(modifier = Modifier.background(Color.White))
                    {
                        PEQRCode(id = studentInfo!!.stdNumber, name = studentInfo!!.stuName)
                    }

                }
            }
            if (showLogoutConfirm) {
                OverlayDialog(
                    show = showLogoutConfirm,
                    title = "确认退出账号",
                    onDismissRequest = { showLogoutConfirm = false }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showLogoutConfirm=false }) {
                            Text(text = "取消")
                        }

                        Spacer(modifier = Modifier.size(16.dp))
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = {
                                viewModel.logout()
                                showLogoutConfirm = false
                            },) {
                            Text(text = "确认", color = MiuixTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
            if (showLoginDialog) {
                PELoginDialog(
                    onDismiss = {
                        showLoginDialog = false
                        viewModel.onLoginDialogDismissed()
                    },
                    onLogin = { username, password ->
                        viewModel.login(
                            username, password,
                            onSuccess = {
                                showLoginDialog = false
                                viewModel.onLoginDialogDismissed()
                            },
                            onError = {}
                        )
                    },
                    loading = loading,
                    error = error
                )
            }
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = loading,
            onRefresh = { viewModel.loadScoreList() },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
            modifier = Modifier.fillMaxSize().padding(
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = 0.dp
                )
            )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
                contentPadding = appPageContentPadding(
                    innerPadding = PaddingValues(),
                    outerPadding = appScaffoldPadding,
                    extraTop = 12.dp,
                    extraStart = 16.dp,
                    extraEnd = 16.dp,
                ),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                overscrollEffect = overscrollEffect
            ) {
                if (!loggedIn) {
                    item {
                        SmallTitle(text = "账号")
                        AppCard {
                            ArrowPreference(
                                title = "登录",
                                summary = "用于获取体测成绩",
                                onClick = { showLoginDialog = true })
                        }
                    }
                }

                items(yearScores, key = { it.schoolYear }) { score ->
                    Box(
                        modifier = Modifier.animateItem(
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { if (score.isFree == 0) onNavigateToDetail(score.schoolYear) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val nextYear = score.schoolYear.toInt() + 1
                                    Text(
                                        text = "${score.schoolYear}-${nextYear}学年",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "已测 ${score.done}/${score.nums}",
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                }
                                Text(
                                    text = if (score.isFree == 0) "${score.total}分" else "免测",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PELoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    loading: Boolean,
    error: String?
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    OverlayDialog(
        show = true,
        title = "登录体测系统",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                label = "学号",
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = ContentType.Username
                }
            )
            TextField(
                label = "密码",
                value = password,
                onValueChange = { password = it },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = ContentType.Password
                }
            )
            if (error != null) {
                Text(
                    text = error,
                    color = MiuixTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { onLogin(username, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotBlank() && password.isNotBlank() && !loading,
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(
                    text = if (loading) "登录中..." else "登录",
                    color = MiuixTheme.colorScheme.onPrimary
                )
            }
        }
    }
}