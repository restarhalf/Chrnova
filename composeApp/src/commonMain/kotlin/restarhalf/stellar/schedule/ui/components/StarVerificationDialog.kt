package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun StarVerificationDialog(
    show: Boolean,
    username: TextFieldValue,
    isVerifying: Boolean,
    error: String?,
    onUsernameChange: (TextFieldValue) -> Unit,
    onVerify: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    val colors = MiuixTheme.colorScheme
    WindowDialog(
        show = true,
        modifier = Modifier,
        title = "GitHub Star 验证",
        titleColor = DialogDefaults.titleColor(),
        summary = "请先 star Chrnova 仓库后才能使用此功能",
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = null,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextField(
                    label = "GitHub 用户名",
                    value = username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(
                        text = it,
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.error,
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    enabled = username.text.isNotBlank() && !isVerifying,
                    onClick = onVerify,
                ) {
                    Text(
                        text = if (isVerifying) "验证中..." else "验证",
                        color = colors.onPrimary,
                    )
                }
            }
        },
    )
}
