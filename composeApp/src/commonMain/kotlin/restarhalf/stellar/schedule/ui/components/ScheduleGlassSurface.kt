package restarhalf.stellar.schedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.haooz.softglass.GlassPreset
import com.haooz.softglass.glass.SoftGlassStyle
import com.haooz.softglass.glass.untransformGlassOrigin
import restarhalf.stellar.schedule.domain.model.CardEdgeThickness
import restarhalf.stellar.schedule.domain.model.CardRefractionLevel
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings

/**
 * 卡片高光描边的一组口径。
 *
 * 注意：只动光照，不动折射 / 反射 / 边缘厚度 —— 那些是「同一块材质」的部分。
 */
private const val CARD_LIGHT_INTENSITY = 0.8f
private const val CARD_LIGHT_OPPOSITE = 0.45f
private const val CARD_LIGHT_ANGLE_RANGE = 1.78f
private const val CARD_LIGHT_FALLOFF = 0.86f

/**
 * 页面内卡片的柔光样式：沿用 Nexio `GlassPreset.Default.style`（`NoneFill` + `TokenTone`，`unshade = 0f`），
 * 仅覆盖模糊半径、折射强度、边缘厚度、高光描边与卡片光照口径。
 *
 * 不能套用 `ThemeFill`（否则 `unshade = 0.55f` 会在 shader 内部把玻璃强行混成半实白色，
 * 把壁纸透视和边缘折射盖死）。
 */
@Composable
internal fun rememberScheduleGlassStyle(
    cardBlurRadiusPx: Float = BackgroundSettingsPort.DEFAULT_CARD_BLUR_PX,
    cardRefraction: CardRefractionLevel = CardRefractionLevel.DEFAULT,
    cardEdgeThickness: CardEdgeThickness = CardEdgeThickness.DEFAULT,
    cardRim: Boolean = BackgroundSettingsPort.DEFAULT_CARD_RIM,
): SoftGlassStyle {
    val base = GlassPreset.Default.style
    val glassRefractionScale = cardRefraction.scale
    val glassBlurPx = cardBlurRadiusPx.coerceIn(0f, BackgroundSettingsPort.MAX_CARD_BLUR_PX)
    val glassEdgePx = base.edgeWidth * cardEdgeThickness.scale
    return remember(
        glassRefractionScale,
        glassBlurPx,
        glassEdgePx,
        cardRim,
    ) {
        base.copy(
            refractionScale = glassRefractionScale,
            smallBlurRadius = glassBlurPx,
            edgeWidth = glassEdgePx,
            edgeHighlight = cardRim,
            lightIntensity = CARD_LIGHT_INTENSITY,
            lightOppositeIntensity = CARD_LIGHT_OPPOSITE,
            lightAngleRange = CARD_LIGHT_ANGLE_RANGE,
            edgeLightFalloff = CARD_LIGHT_FALLOFF,
        )
    }
}

/**
 * 浮层专用的压暗档：只改 [SoftGlassStyle.darker]，其余参数仍取 [rememberScheduleGlassStyle]。
 *
 * 为什么浮层需要单独一档：实时路径的大模糊底（`contentBig`）只能采控件自身矩形内的内容，
 * 而网格卡片采的是金字塔最粗一级 —— 整张壁纸的极粗平均。浮层压在亮色卡片上时，
 * 底色天然比卡片亮一档；`darker` 只在亮部区间（`darkerRange`）生效，正好压掉这一段，
 * 暗部与整体色相不动。
 */
private const val OVERLAY_GLASS_DARKER = 0.2f

@Composable
internal fun rememberOverlayGlassStyle(scheduleGlassStyle: SoftGlassStyle): SoftGlassStyle {
    return remember(scheduleGlassStyle) {
        scheduleGlassStyle.copy(darker = OVERLAY_GLASS_DARKER)
    }
}

/**
 * 页面级柔光采样源：首页大卡、分界带等共用同一实例。
 *
 * 只在外壳创建一次 —— `prepareSoftGlassBackdrop` 会烘焙一整座壁纸金字塔，多建一份就是
 * 双倍显存 + 一次全尺寸像素处理；共用实例也让两者的玻璃参数不可能漂移。
 *
 * @param originTransform 绘制期求值的「窗口坐标 → 壁纸层逻辑坐标」修正。
 *   祖先 `graphicsLayer` 的缩放 / 平移（外观页缩到 0.65、sheet 上移、主层转场）会被
 *   `positionInWindow()` 算进去，而 `uTexMap` 是按未变换的壁纸层推的仿射，
 *   不还原就会采歪。
 */
@Composable
internal fun rememberScheduleGlassSurface(
    wallpaperBitmap: ImageBitmap?,
    wallpaperScale: Float,
    wallpaperOffset: Offset,
    screenWidthPx: Float,
    screenHeightPx: Float,
    wallpaperBrightness: Float,
    wallpaperAlpha: Float = 1f,
    baseColor: Color = Color.Transparent,
    style: SoftGlassStyle,
    originTransform: (Offset) -> Offset,
    enabled: Boolean = AppMaterialSettings.glassAvailable(),
): SoftGlassSurface? {
    // 无材质档：卡片走普通半透明表面，采样源一个都用不到 ——
    // 别为它白烘一座金字塔（一次整图降采样 + 7 级显存）
    if (!enabled) return null
    return rememberSoftGlassSurface(
        wallpaperBitmap = wallpaperBitmap,
        wallpaperScale = wallpaperScale,
        wallpaperOffset = wallpaperOffset,
        screenWidthPx = screenWidthPx,
        screenHeightPx = screenHeightPx,
        wallpaperBrightness = wallpaperBrightness,
        wallpaperAlpha = wallpaperAlpha,
        baseColor = baseColor,
        style = style,
        originTransform = originTransform,
    )
}

/**
 * 构造「窗口坐标 → 壁纸层逻辑坐标」的修正函数。
 *
 * @param scale 祖先层当前的缩放（外观页 0.65 → 1）
 * @param translationY 祖先层当前的纵向位移（px）
 * @param pivotX / pivotY 缩放的支点（窗口坐标，px）
 */
internal fun makeGlassOriginTransform(
    scale: () -> Float,
    translationY: () -> Float,
    pivotX: Float,
    pivotY: Float,
): (Offset) -> Offset = { origin ->
    untransformGlassOrigin(
        origin = origin,
        scale = scale(),
        translationY = translationY(),
        pivotX = pivotX,
        pivotY = pivotY,
    )
}
