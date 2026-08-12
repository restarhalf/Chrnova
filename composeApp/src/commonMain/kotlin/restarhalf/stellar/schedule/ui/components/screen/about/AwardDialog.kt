package restarhalf.stellar.schedule.ui.components.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 赞赏对话框组件
 * 
 * 显示赞赏选项，支持微信和支付宝两种赞赏方式。
 * 
 * @param show 是否显示
 * @param onWxpay 微信赞赏回调
 * @param onAlipay 支付宝赞赏回调
 */
@Composable
fun AwardDialog(show: Boolean, onDismissRequest: () -> Unit, onWxpay: () -> Unit, onAlipay: () -> Unit) {
    WindowDialog(
        show = show,
        modifier = Modifier,
        title = "赞赏作者",
        titleColor = DialogDefaults.titleColor(),
        summary = "赞赏以支持继续更新",
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismissRequest,
        onDismissFinished = null,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
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