package restarhalf.stellar.schedule.ui.components.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 教务系统网页对话框组件
 * 
 * 提供选择教务系统入口的对话框，支持：
 * - 手机端教务系统
 * - PC端教务系统
 * 
 * @param show 是否显示
 * @param onPc 打开PC端教务系统回调
 * @param onMobile 打开手机端教务系统回调
 */
@Composable
fun JwxtWebDialog(show: Boolean, onDismissRequest: () -> Unit, onPc: () -> Unit, onMobile: () -> Unit) {
    OverlayDialog(
        show = show,
        modifier = Modifier,
        title = "打开教务系统",
        titleColor = DialogDefaults.titleColor(),
        summary = "选择你要使用的教务入口",
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismissRequest,
        onDismissFinished = null,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        renderInRootScaffold = true,
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(modifier = Modifier.weight(1f), onClick = onMobile) {
                    Text(text = "手机端")
                }

                Spacer(modifier = Modifier.size(16.dp))

                Button(modifier = Modifier.weight(1f), onClick = onPc) {
                    Text(text = "电脑端")
                }
            }
        },
    )
}