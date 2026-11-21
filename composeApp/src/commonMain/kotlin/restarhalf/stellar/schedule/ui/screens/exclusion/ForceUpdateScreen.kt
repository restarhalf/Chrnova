package restarhalf.stellar.schedule.ui.screens.exclusion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.ui.icons.AppIcon
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.NavigationBackHandlerFor

/**
 * 重要更新全屏提示页：覆盖全屏展示版本变更与更新日志，支持立即更新或稍后更新。
 */
@Composable
fun ForceUpdateScreen(
    currentVersionName: String,
    updateInfo: AppUpdateInfo,
    onStartDownload: (AppUpdateInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    NavigationBackHandlerFor(
        enabled = true,
        onBackCompleted = onDismiss,
        onBackCancelled = {},
    )

    val scrollState = rememberScrollState()
    val colors = MiuixTheme.colorScheme
    val versionLine = remember(currentVersionName, updateInfo.latestVersion) {
        val current = formatVersionLabel(currentVersionName)
        val latest = formatVersionLabel(updateInfo.latestVersion)
        if (current.isNotEmpty() && latest.isNotEmpty()) {
            "$current → $latest"
        } else {
            latest.ifEmpty { current }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 480.dp)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Icon(
                imageVector = AppIcon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "发现重要版本更新",
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )

            if (versionLine.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = versionLine,
                    style = MiuixTheme.textStyles.body2,
                    color = colors.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "本次为重要版本更新，建议尽快更新至最新版本。",
                style = MiuixTheme.textStyles.body2,
                color = colors.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                ) {
                    Text(
                        text = updateInfo.changelog.ifBlank { "本次更新包含重要修复与改进，请尽快更新。" },
                        style = MiuixTheme.textStyles.body2,
                        color = colors.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                text = "立即更新",
                onClick = { onStartDownload(updateInfo) },
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                text = "稍后更新",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatVersionLabel(version: String): String {
    val trimmed = version.trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.startsWith("v", ignoreCase = true)) trimmed else "v$trimmed"
}
