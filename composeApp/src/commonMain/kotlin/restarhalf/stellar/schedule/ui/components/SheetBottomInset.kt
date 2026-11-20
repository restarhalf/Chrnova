package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 弹窗内容的底部留白。
 *
 * 弹窗外壳整体下移了 `statusBars + 5.dp`（见 `BlurBottomSheet`），内容底部要把这段位移补回来，
 * 再补手势条/导航栏高度，最后留 [phoneExtra] 呼吸空间，最后一项才不会贴着屏幕底边。
 * 原先固定 60dp 的额外留白在三键导航机上叠出底部一大块空白，改为按实际导航栏高度计算。
 */
@Composable
fun sheetContentBottomInset(phoneExtra: Dp = 16.dp): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        phoneExtra
