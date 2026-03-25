package restarhalf.stellar.schedule.ui.components.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.DialogDefaults
import top.yukonga.miuix.kmp.extra.SuperDialog

@Composable
fun JwxtWebDialog(show: MutableState<Boolean>, onPc: () -> Unit, onMobile: () -> Unit) {
    SuperDialog(
        show = show.value,
        modifier = Modifier,
        title = "打开教务系统",
        titleColor = DialogDefaults.titleColor(),
        summary = "选择你要使用的教务入口",
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = { show.value = false },
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