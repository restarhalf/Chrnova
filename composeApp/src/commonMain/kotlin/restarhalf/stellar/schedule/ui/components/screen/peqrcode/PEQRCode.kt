package restarhalf.stellar.schedule.ui.components.screen.peqrcode

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
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
        painter = rememberQrCodePainter(codeContent),
        contentDescription = null,
    )
}