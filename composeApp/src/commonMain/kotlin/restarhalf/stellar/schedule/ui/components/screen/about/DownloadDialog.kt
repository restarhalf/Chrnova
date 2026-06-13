package restarhalf.stellar.schedule.ui.components.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.core.text.DecimalFormatter
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 下载进度对话框组件
 * 
 * 显示APK下载进度，支持停止下载和后台下载。
 * 
 * @param show 是否显示
 * @param downloadProgress 下载进度（0.0-1.0）
 * @param onStop 停止下载回调
 * @param onBackGround 后台下载回调
 */
@Composable
fun DownloadDialog(
    show: MutableState<Boolean>,
    downloadProgress: Float,
    onStop: () -> Unit,
    onBackGround: () -> Unit,
) {
    WindowDialog(
        show = show.value,
        modifier = Modifier,
        title = "下载中",
        titleColor = DialogDefaults.titleColor(),
        summary = null,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = null,
        onDismissFinished = null,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            if (downloadProgress >= 100.00f) {
                show.value = false
            }

            val normalizedProgress = (downloadProgress / 100f).coerceIn(0f, 1f)
            Column(modifier = Modifier.padding(bottom = 0.dp)) {
                Text(text = "下载进度 ${DecimalFormatter.format(downloadProgress, 2)}%")
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(progress = normalizedProgress)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 15.dp)
                ) {
                    Button(modifier = Modifier.weight(1f), onClick = onStop) {
                        Text(text = "取消")
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Button(
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = onBackGround
                    ) {
                        Text(text = "后台下载", color = MiuixTheme.colorScheme.onPrimary)
                    }
                }
            }
        })
}
