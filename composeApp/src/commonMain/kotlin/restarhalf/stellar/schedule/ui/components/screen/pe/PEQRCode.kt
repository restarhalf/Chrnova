package restarhalf.stellar.schedule.ui.components.screen.pe

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import io.ktor.http.encodeURLParameter

@Composable
fun PEQRCode(
    id: String,
    name: String,
){
    val urlName = name.encodeURLParameter()
    val codeContent="id=$id&name=$urlName"
    Image(
        modifier = Modifier.size(350.dp),
        painter = rememberQrCodePainter(data=codeContent, colors = QrColors(
        )),
        contentDescription = null,
    )
}