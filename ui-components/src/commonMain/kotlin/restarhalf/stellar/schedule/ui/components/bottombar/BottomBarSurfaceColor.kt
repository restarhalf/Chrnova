package restarhalf.stellar.schedule.ui.components.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底栏底色。
 *
 * 玻璃档下这不是一层"实色罩"，而是压在玻璃之上的**薄底色**：按材质档位降一档
 * （浅色 -0.2 / 深色 -0.32），既保住图标与文字的可读性，又不把折射与边光盖掉。
 * 无材质档才是不透明的实色面。
 */
@Composable
fun bottomBarSurfaceColor(): Color {
    val isLight = MiuixTheme.colorScheme.background.luminance() >= 0.5f
    return when {
        MiuixTheme.isDynamicColor ->
            MiuixTheme.colorScheme.surfaceContainer.copy(
                alpha = AppMaterialSettings.glassSurfaceAlpha(0.5f, isLight),
            )

        isLight ->
            Color(0xFFFAFAFA).copy(
                alpha = AppMaterialSettings.glassSurfaceAlpha(0.5f, isLight = true),
            )

        else ->
            Color(0xFF242424).copy(
                alpha = AppMaterialSettings.glassSurfaceAlpha(0.6f, isLight = false),
            )
    }
}
