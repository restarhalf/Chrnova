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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.PersonalInfoEditCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Logout
import restarhalf.stellar.schedule.ui.chrome.AppPageTopBar
import restarhalf.stellar.schedule.ui.chrome.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.chrome.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PersonalInfoViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ProfileScreen(
    peAuthProfile: PEAuthProfile?,
    jwxtAuthProfile: JwxtAuthProfile?,
    onBack: () -> Unit,
    onLogoutJW: () -> Unit = {},
    onLogoutPE: () -> Unit = {},
    pictureSelectorHost: @Composable (
        show: Boolean,
        onDismissRequest: () -> Unit,
        onPicked: (String) -> Unit,
        outputWidthPx: Int?,
        outputHeightPx: Int?,
    ) -> Unit = { _, _, _, _, _ -> },
    personalInfoViewModel: PersonalInfoViewModel,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val appScaffoldPadding = LocalAppScaffoldPadding.current

    val isLoggedIn = jwxtAuthProfile?.userNo?.isNotBlank() == true
    val isPeLoggedIn = peAuthProfile?.stdNumber?.isNotBlank() == true

    LaunchedEffect(isLoggedIn, isPeLoggedIn) {
        if (!isLoggedIn && !isPeLoggedIn) {
            delay(300.milliseconds)
            onBack()
        }
    }

    var showLogoutJwxtConfirm by remember { mutableStateOf(false) }
    var showLogoutPEConfirm by remember { mutableStateOf(false) }
    var showPictureSelector by remember { mutableStateOf(false) }
    val personalInfoUiState by personalInfoViewModel.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

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
                            tint = colors.onBackground,
                        )
                    }
                },
            )
        },
        popupHost = {

            if (showLogoutJwxtConfirm) {
                WindowDialog(
                    show = showLogoutJwxtConfirm,
                    title = "确认退出教务系统",
                    summary = "退出后需要重新登录才能同步课表和考务",
                    onDismissRequest = { showLogoutJwxtConfirm = false }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showLogoutJwxtConfirm = false }
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = {
                                onLogoutJW()
                                showLogoutJwxtConfirm = false
                            }
                        ) {
                            Text(text = "确认退出", color = colors.onPrimary)
                        }
                    }
                }
            }

            if (showLogoutPEConfirm) {
                WindowDialog(
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
                                onLogoutPE()
                                showLogoutPEConfirm = false
                            }
                        ) {
                            Text(text = "确认", color = colors.onPrimary)
                        }
                    }
                }
            }

            pictureSelectorHost(
                showPictureSelector,
                { showPictureSelector = false },
                { croppedUri ->
                    personalInfoViewModel.saveAvatar(croppedUri)
                    showPictureSelector = false
                },
                512,
                512,
            )
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
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            item {
                SmallTitle(text = "个人信息")
                PersonalInfoEditCard(
                    avatarUri = personalInfoUiState.avatarUri,
                    nickname = personalInfoUiState.nickname,
                    onAvatarClick = { showPictureSelector = true },
                    onAvatarClear = { personalInfoViewModel.clearAvatar() },
                    onNicknameChanged = { nickname ->
                        personalInfoViewModel.saveNickname(nickname)
                    },
                )
            }


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
                            ProfileInfoRow(label = "姓名", value = jwxtAuthProfile.name)
                            HorizontalDivider()
                            ProfileInfoRow(label = "学号", value = jwxtAuthProfile.userNo)
                            HorizontalDivider()
                            ProfileInfoRow(
                                label = "班级",
                                value = jwxtAuthProfile.clsName.takeIf { it.isNotBlank() } ?: "暂无")
                            HorizontalDivider()
                            ProfileInfoRow(
                                label = "学院",
                                value = jwxtAuthProfile.academyName.takeIf { it.isNotBlank() }
                                    ?: "暂无")
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLogoutJwxtConfirm = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "退出登录",
                                    style = MiuixTheme.textStyles.body1,
                                    color = colors.error,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Logout,
                                    contentDescription = "退出教务系统",
                                    tint = colors.error,
                                )
                            }
                        }
                    }
                }
            }


            item {
                if (isPeLoggedIn) {
                    SmallTitle(text = "体测平台")
                    AppCard(modifier = Modifier.fillMaxWidth()) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ProfileInfoRow(label = "姓名", value = peAuthProfile.stuName)
                            HorizontalDivider()
                            ProfileInfoRow(label = "学号", value = peAuthProfile.stdNumber)
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
                                    style = MiuixTheme.textStyles.body1,
                                    color = colors.error,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Logout,
                                    contentDescription = "退出体测平台",
                                    tint = colors.error,
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
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = colors.onSurfaceVariantSummary,
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            color = colors.onBackground,
        )
    }
}
