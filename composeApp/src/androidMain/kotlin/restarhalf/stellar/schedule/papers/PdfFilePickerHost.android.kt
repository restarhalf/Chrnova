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

@Composable
fun PdfFilePickerHost(
    onPicked: (ByteArray, String, String) -> Unit,
) {
    val context = LocalContext.current
    val pendingUri = remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingUri.value = uri
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("application/pdf"))
    }

    val currentUri = pendingUri.value
    if (currentUri != null) {
        LaunchedEffect(currentUri) {
            val bytes = context.contentResolver.openInputStream(currentUri)?.use { it.readBytes() }
            val fileName = getFileName(context, currentUri)
            val mimeType = context.contentResolver.getType(currentUri) ?: "application/pdf"
            if (bytes != null) {
                onPicked(bytes, fileName, mimeType)
            }
            pendingUri.value = null
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
