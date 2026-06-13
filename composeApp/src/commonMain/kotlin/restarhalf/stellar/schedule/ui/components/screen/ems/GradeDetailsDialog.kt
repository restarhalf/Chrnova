package restarhalf.stellar.schedule.ui.components.screen.ems

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 成绩详情对话框组件
 * 
 * 显示课程成绩的详细信息，以对话框形式展示。
 * 
 * @param show 是否显示
 * @param title 对话框标题（课程名称）
 * @param summary 成绩详情摘要文本
 */
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