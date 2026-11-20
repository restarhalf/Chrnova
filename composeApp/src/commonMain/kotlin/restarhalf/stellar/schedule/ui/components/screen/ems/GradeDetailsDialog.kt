package restarhalf.stellar.schedule.ui.components.screen.ems

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import restarhalf.stellar.schedule.ui.components.LocalLiquidGlassBackdrop
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun GradeDetailsDialog(show: Boolean, onDismissRequest: () -> Unit, title: String, summary: String) {
    OverlayDialog(
        show = show,
        liquidGlassBackdrop = LocalLiquidGlassBackdrop.current,
        modifier = Modifier,
        title = title,
        titleColor = DialogDefaults.titleColor(),
        summary = summary,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismissRequest,
        onDismissFinished = null,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {})
}
