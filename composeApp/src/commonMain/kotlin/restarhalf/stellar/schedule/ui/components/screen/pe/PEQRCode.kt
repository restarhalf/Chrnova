package restarhalf.stellar.schedule.ui.components.screen.pe

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import io.ktor.http.encodeURLParameter

/**
 * 体育系统二维码组件
 * 
 * 生成包含学生信息的二维码，用于体育系统登录或信息查询。
 * 
 * @param id 学生ID
 * @param name 学生姓名
 */
@Composable
fun PEQRCode(
    id: String,
    name: String,
){
    val urlName = name.encodeURLParameter()
    val codeContent="id=$id&name=$urlName"
    Box(
        modifier = Modifier.background(Color.White)
    ){
        Image(
            modifier = Modifier.size(350.dp),
            painter = rememberQrCodePainter(
                data = codeContent,
            ),
            contentDescription = null,
        )
    }

}