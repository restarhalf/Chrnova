package restarhalf.stellar.schedule.ui.components.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun AwardDialog(show: MutableState<Boolean>, onWxpay: () -> Unit, onAlipay: () -> Unit) {
    OverlayDialog(
        show = show.value,
        modifier = Modifier,
        title = "赞赏作者",
        titleColor = DialogDefaults.titleColor(),
        summary = "赞赏以支持继续更新",
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
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(Color(0xFF06C360)),
                    onClick = onWxpay,
                ) {
                    Text(text = "微信")
                }

                Spacer(modifier = Modifier.size(16.dp))

                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(Color(0xFF1077FE)),
                    onClick = onAlipay,
                ) {
                    Text(text = "支付宝")
                }
            }
        },
    )
}