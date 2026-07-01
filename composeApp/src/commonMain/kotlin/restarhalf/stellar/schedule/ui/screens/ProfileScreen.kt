package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.domain.model.AuthProfile
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Logout
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProfileScreen(
    peVm: PEViewModel,
    authProfile: AuthProfile?,
    onBack: () -> Unit,
    onLogoutJW: () -> Unit = {},
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val peUiState by peVm.uiState.collectAsState()
    val peStudentInfo = peUiState.studentInfo

    val isLoggedIn = authProfile?.userNo?.isNotBlank() == true
    val isPeLoggedIn = peVm.isLoggedIn()

    LaunchedEffect(isLoggedIn, isPeLoggedIn) {
        if (!isLoggedIn && !isPeLoggedIn) {
            onBack()
        }
    }

    var showLogoutJWConfirm by remember { mutableStateOf(false) }
    var showLogoutPEConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "个人资料",
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
        popupHost = {
            // 教务系统退出确认
            if (showLogoutJWConfirm) {
                OverlayDialog(
                    show = showLogoutJWConfirm,
                    title = "确认退出教务系统",
                    summary = "退出后需要重新登录才能同步课表和考务",
                    onDismissRequest = { showLogoutJWConfirm = false }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showLogoutJWConfirm = false }
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = {
                                onLogoutJW()
                                showLogoutJWConfirm = false
                            }
                        ) {
                            Text(text = "确认退出", color = MiuixTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
            // 体测系统退出确认
            if (showLogoutPEConfirm) {
                OverlayDialog(
                    show = showLogoutPEConfirm,
                    title = "确认退出体测平台",
                    summary = "退出后需要重新登录才能查看体测成绩",
                    onDismissRequest = { showLogoutPEConfirm = false }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showLogoutPEConfirm = false }
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = {
                                peVm.logout()
                                showLogoutPEConfirm = false
                            }
                        ) {
                            Text(text = "确认", color = MiuixTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = paddingValues,
                outerPadding = appScaffoldPadding,
                extraTop = 12.dp,
                extraStart = 16.dp,
                extraEnd = 16.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 教务系统信息
            item {
                if (isLoggedIn) {
                    SmallTitle(text = "教务系统")
                    AppCard(modifier = Modifier.fillMaxWidth()) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ProfileInfoRow(label = "姓名", value = authProfile.name)
                            HorizontalDivider()
                            ProfileInfoRow(label = "学号", value = authProfile.userNo)
                            HorizontalDivider()
                            ProfileInfoRow(
                                label = "班级",
                                value = authProfile.clsName.takeIf { it.isNotBlank() } ?: "暂无")
                            HorizontalDivider()
                            ProfileInfoRow(
                                label = "学院",
                                value = authProfile.academyName.takeIf { it.isNotBlank() }
                                    ?: "暂无")
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLogoutJWConfirm = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "退出登录",
                                    fontSize = 15.sp,
                                    color = MiuixTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Logout,
                                    contentDescription = "退出教务系统",
                                    tint = MiuixTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            // 体测系统信息
            item {
                if (isPeLoggedIn && peStudentInfo != null) {
                    SmallTitle(text = "体测平台")
                    AppCard(modifier = Modifier.fillMaxWidth()) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ProfileInfoRow(label = "姓名", value = peStudentInfo.stuName)
                            HorizontalDivider()
                            ProfileInfoRow(label = "学号", value = peStudentInfo.stdNumber)
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLogoutPEConfirm = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "退出登录",
                                    fontSize = 15.sp,
                                    color = MiuixTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Logout,
                                    contentDescription = "退出体测平台",
                                    tint = MiuixTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onBackground,
        )
    }
}
