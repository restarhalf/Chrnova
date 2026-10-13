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
    multiple: Boolean = false,
    onPicked: (List<PickedAttachment>) -> Unit,
) {
    val context = LocalContext.current

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
