package restarhalf.stellar.schedule.ui.screens.announcement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.ui.icons.Close
import restarhalf.stellar.schedule.ui.image.ZoomableAsyncImage
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text

/**
 * 公告图片全屏看图器（QQ 风格）。
 *
 * 黑底铺满全屏，[ZoomableAsyncImage] 提供捏合/双击缩放与平移；点按图片切换
 * 顶部状态栏（关闭 + 保存）显隐；保存按钮经 [saveImage] 把网络图存到相册，
 * 结果经 [showMessage] 提示。仅当 [canSaveImage] 为真时展示保存入口。
 */
@Composable
fun AnnouncementImageViewerScreen(
    url: String,
    alt: String?,
    canSaveImage: Boolean,
    saveImage: suspend (String) -> Boolean,
    showMessage: (String) -> Unit,
    onBack: () -> Unit,
) {
    var chromeVisible by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .navigationBarsPadding(),
    ) {
        ZoomableAsyncImage(
            url = url,
            contentDescription = alt,
            modifier = Modifier.fillMaxSize(),
            onTap = { chromeVisible = !chromeVisible },
        )

        if (chromeVisible) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                    )
                }

                if (canSaveImage) {
                    Box(
                        modifier =
                            Modifier
                                .clickable(enabled = !saving) {
                                    scope.launch {
                                        saving = true
                                        try {
                                            val ok = saveImage(url)
                                            showMessage(if (ok) "已保存到相册" else "保存失败，请重试")
                                        } finally {
                                            saving = false
                                        }
                                    }
                                }
                                .padding(12.dp),
                    ) {
                        Text(
                            text = if (saving) "保存中" else "保存",
                            color = if (saving) Color.White.copy(alpha = 0.5f) else Color.White,
                        )
                    }
                }
            }
        }
    }
}
