package restarhalf.stellar.schedule.ui.components.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 更新确认对话框组件
 * 
 * 显示新版本信息，询问用户是否下载更新。
 * 包含更新日志和下载按钮。
 * 
 * @param show 是否显示
 * @param pendingUpdate 待处理的更新信息
 * @param onStartDownload 开始下载回调
 * @param onLater 稍后更新回调
 */
@Composable
fun UpdateConfirmDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    pendingUpdate: AppUpdateInfo?,
    onStartDownload: (AppUpdateInfo) -> Unit,
    onLater: (() -> Unit)? = null,
) {
    WindowDialog(
        show = show,
        modifier = Modifier,
        title = "发现新版本 ${pendingUpdate?.latestVersion.orEmpty()}",
        titleColor = DialogDefaults.titleColor(),
        summary = null,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = {
            onLater?.invoke()
            onDismissRequest()
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
                            onDismissRequest()
                        }) {
                        Text(text = "稍后")
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Button(
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = {
                            pendingUpdate?.let { onStartDownload(it) }
                            onDismissRequest()
                        }) {
                        Text(text = "下载")
                    }
                }
            }
        })
}
