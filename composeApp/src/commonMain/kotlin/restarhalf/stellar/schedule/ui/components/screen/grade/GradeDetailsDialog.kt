package restarhalf.stellar.schedule.ui.components.screen.grade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun GradeDetailsDialog(show: MutableState<Boolean>, title: String, summary: String) {
    OverlayDialog(
        show = show.value,
        modifier = Modifier,
        title = title,
        titleColor = DialogDefaults.titleColor(),
        summary = summary,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = { show.value = false },
        onDismissFinished = null,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        renderInRootScaffold = true,
        content = {})
}