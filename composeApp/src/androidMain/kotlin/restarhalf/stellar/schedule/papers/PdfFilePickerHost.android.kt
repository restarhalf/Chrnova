package restarhalf.stellar.schedule.papers

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 文档/图片选择器宿主（Android）。
 *
 * 每次进入组合都会拉起选择器；选择结束（含取消）必定回调 [onPicked]，
 * 取消时列表为空，便于调用方收起宿主、下次再点重新唤起。
 *
 * @param multiple 是否允许多选
 * @param onPicked 选择完成后一次性回调全部文件（取消为空列表）
 */
@Composable
fun PdfFilePickerHost(
    multiple: Boolean = false,
    onPicked: (List<PickedAttachment>) -> Unit,
) {
    val context = LocalContext.current
    // null=尚未返回；空列表=取消；非空=已选
    val pendingUris = remember { mutableStateOf<List<Uri>?>(null) }

    val singleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pendingUris.value = listOfNotNull(uri)
    }
    val multiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        pendingUris.value = uris.orEmpty()
    }

    LaunchedEffect(Unit) {
        val mimeTypes = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png",
        )
        if (multiple) {
            multiLauncher.launch(mimeTypes)
        } else {
            singleLauncher.launch(mimeTypes)
        }
    }

    val uris = pendingUris.value
    if (uris != null) {
        LaunchedEffect(uris) {
            val picked = uris.mapNotNull { uri ->
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@mapNotNull null
                val fileName = getFileName(context, uri)
                val mimeType = context.contentResolver.getType(uri) ?: guessMimeFromName(fileName)
                PickedAttachment(bytes = bytes, name = fileName, mime = mimeType)
            }
            pendingUris.value = null
            onPicked(picked)
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        if (nameIndex >= 0) it.getString(nameIndex) else "document.pdf"
    } ?: "document.pdf"
}

private fun guessMimeFromName(name: String): String = when {
    name.endsWith(".doc", ignoreCase = true) -> "application/msword"
    name.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
    name.endsWith(".png", ignoreCase = true) -> "image/png"
    else -> "application/pdf"
}
