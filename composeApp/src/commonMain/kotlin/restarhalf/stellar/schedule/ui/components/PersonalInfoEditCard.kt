package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PersonalInfoEditCard(
    avatarUri: String?,
    nickname: String?,
    onAvatarClick: () -> Unit,
    onAvatarClear: () -> Unit,
    onNicknameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showClearAvatarDialog by remember { mutableStateOf(false) }

    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 头像点击区域
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AvatarImage(
                    avatarUri = avatarUri,
                    contentDescription = "点击更换头像",
                    size = 80.dp,
                    modifier = Modifier.clickable(onClick = onAvatarClick)
                )
                if (avatarUri != null) {
                    Text(
                        text = "清除",
                        fontSize = 12.sp,
                        color = colors.error,
                        modifier = Modifier.clickable { showClearAvatarDialog = true }
                    )
                }
            }

            // 昵称点击区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showNicknameDialog = true },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = nickname ?: "点击设置昵称",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (nickname != null) colors.onBackground else colors.onSurfaceVariantSummary,
                )
                Text(
                    text = "点击编辑",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
        }
    }

    // 昵称编辑对话框
    if (showNicknameDialog) {
        NicknameEditDialog(
            initialNickname = nickname ?: "",
            onDismiss = { showNicknameDialog = false },
            onConfirm = { newNickname ->
                onNicknameChanged(newNickname)
                showNicknameDialog = false
            }
        )
    }

    // 清除头像确认对话框
    if (showClearAvatarDialog) {
        OverlayDialog(
            show = true,
            title = "清除头像",
            summary = "确定要清除头像吗？",
            onDismissRequest = { showClearAvatarDialog = false }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { showClearAvatarDialog = false }
                ) {
                    Text(text = "取消")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = {
                        onAvatarClear()
                        showClearAvatarDialog = false
                    }
                ) {
                    Text(text = "确定", color = colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun NicknameEditDialog(
    initialNickname: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var nickname by remember { mutableStateOf(initialNickname) }
    val colors = MiuixTheme.colorScheme
    
    OverlayDialog(
        show = true,
        title = "设置昵称",
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextField(
                value = nickname,
                onValueChange = { 
                    if (it.length <= 10) {
                        nickname = it 
                    }
                },
                label = "昵称",
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                ) {
                    Text(text = "取消")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = { onConfirm(nickname) }
                ) {
                    Text(text = "确定", color = colors.onPrimary)
                }
            }
        }
    }
}
