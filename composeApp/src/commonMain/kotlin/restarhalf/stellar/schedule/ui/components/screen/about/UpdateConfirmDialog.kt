package restarhalf.stellar.schedule.ui.components.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.DialogDefaults
import top.yukonga.miuix.kmp.extra.WindowDialog

@Composable
fun UpdateConfirmDialog(
    show: MutableState<Boolean>,
    pendingUpdate: AppUpdateInfo?,
    onStartDownload: (AppUpdateInfo) -> Unit,
    onLater: (() -> Unit)? = null,
) {
    WindowDialog(
        show = show.value,
        modifier = Modifier,
        title = "发现新版本 ${pendingUpdate?.latestVersion.orEmpty()}",
        titleColor = DialogDefaults.titleColor(),
        summary = null,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = {
            onLater?.invoke()
            show.value = false
        },
        onDismissFinished = null,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "检测到新版本，是否立即下载并安装？")
                pendingUpdate
                    ?.changelog
                    ?.takeIf { it.isNotBlank() }
                    ?.let { log ->
                        Text(text = "更新说明：${log.take(120)}${if (log.length > 120) "..." else ""}")
                    }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onLater?.invoke()
                            show.value = false
                        }) {
                        Text(text = "稍后")
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Button(
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = {
                            pendingUpdate?.let { onStartDownload(it) }
                            show.value = false
                        }) {
                        Text(text = "下载")
                    }
                }
            }
        })
}
