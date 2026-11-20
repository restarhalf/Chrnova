package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haooz.softglass.GlassPreset
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur
import com.haooz.softglass.backdrop.effects.runtimeShaderEffect
import com.haooz.softglass.glass.applySoftGlass
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings

/** 页面内卡片的壁纸采样层（纯壁纸录制层，不含页面内容）。 */
val LocalWallpaperBackdrop = compositionLocalOf<GlassBackdrop?> { null }

/**
 * 采壁纸层的柔光玻璃表面。
 *
 * 优先使用 [LocalSoftGlassSurface]（7 级预烘焙壁纸模糊金字塔 + 窗口坐标仿射映射），
 * 卡片滚出或滑出屏幕边界时依然采样完整壁纸，不会采到采样层外的透明黑空区；
 * 仅在未提供 [LocalSoftGlassSurface] 时回退到 [drawBackdrop] + [EDGE_SAFE_SOFT_GLASS_LIVE_SHADER]。
 *
 * 绘制顺序与 Nexio 的卡片一致：先玻璃，再叠一层半透明的 [color] 当卡面，
 * 最后画内容。`color` 的 alpha 必须 < 1，否则会把折射与方向光盖死。
 * 无材质档不挂玻璃节点，只留 [color] 实色面。
 *
 * @param cornerRadius 圆角；shader 的 `uCornerRadii` 是单值复制四份，非等角形状取最大值近似。
 *   null 时由 shader 取短边一半（胶囊）。
 */
@Composable
fun Modifier.wallpaperGlass(
    backdrop: GlassBackdrop,
    color: Color,
    shape: Shape,
    cornerRadius: Dp? = null,
): Modifier {
    val glassOn = AppMaterialSettings.glassEffectsEnabled()
    val glassBlur = AppMaterialSettings.glassBlurDp()
    val density = LocalDensity.current
    val radiusPx = remember(cornerRadius, density) {
        cornerRadius?.let { with(density) { it.toPx() } }
    }
    val clipped = this.clip(shape)
    // 无材质档：不挂玻璃节点，只留实色面。
    if (!glassOn) return clipped.background(color, shape)

    val softGlass = LocalSoftGlassSurface.current
    val glassRedraw = LocalGlassRedrawSignal.current
    if (softGlass != null) {
        val glassOrigin = remember { arrayOf(Offset.Zero) }
        val glassModifier =
            remember(softGlass, radiusPx, density, glassRedraw) {
                softGlass.modifier(
                    density = density,
                    origin = { glassOrigin[0] },
                    radiusPx = radiusPx,
                    redraw = glassRedraw,
                )
            }
        return clipped
            .then(GlassOriginTrackerElement(glassOrigin))
            .then(glassModifier)
            .background(color, shape)
    }

    val shapeProvider: () -> Shape = remember(shape) { { shape } }
    // 卡片的玻璃只出折射 / 边光 / 方向光，玻璃本色留空：
    // 卡面颜色由调用方以 background 叠上去（与 Nexio 的卡片同一套做法）。
    val glassStyle = remember {
        GlassPreset.Default.style.copy(
            lightIntensity = 0.8f,
            lightOppositeIntensity = 0.45f,
            lightAngleRange = 1.78f,
            edgeLightFalloff = 0.86f,
        )
    }
    val effects: GlassEffectScope.() -> Unit =
        remember(glassOn, glassBlur, glassStyle, radiusPx) {
            {
                if (glassOn) {
                    blur(glassBlur.dp.toPx(), TileMode.Mirror)
                    padding = 0f
                    runtimeShaderEffect(
                        // 尺寸不同必须分开 key，否则同帧多张卡的 uniform 会互相覆盖
                        key = "CardSoftGlass_${size.width.toInt()}x${size.height.toInt()}",
                        shaderString = EDGE_SAFE_SOFT_GLASS_LIVE_SHADER,
                        uniformShaderName = "content",
                    ) {
                        applySoftGlass(
                            style = glassStyle,
                            width = size.width,
                            height = size.height,
                            radiusPx = radiusPx,
                        )
                    }
                }
            }
        }
    // 玻璃在下、卡面在上：`color` 的 alpha < 1，柔光 shader 的折射 / 边光从它下面透出来。
    return clipped
        .drawBackdrop(
            backdrop = backdrop,
            shape = shapeProvider,
            effects = effects,
            shadow = null,
            onDrawSurface = null,
        )
        .background(color, shape)
}

/** 轻量级窗口坐标追踪节点：仅在窗口坐标变化时更新 origin 并失效重绘，不触发重组。 */
private class GlassOriginTrackerElement(
    val originBox: Array<Offset>,
) : ModifierNodeElement<GlassOriginTrackerNode>() {
    override fun create(): GlassOriginTrackerNode = GlassOriginTrackerNode(originBox)

    override fun update(node: GlassOriginTrackerNode) {
        node.originBox = originBox
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassOriginTracker"
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is GlassOriginTrackerElement && originBox === other.originBox)

    override fun hashCode(): Int = originBox.hashCode()
}

private class GlassOriginTrackerNode(
    var originBox: Array<Offset>,
) : Modifier.Node(), GlobalPositionAwareModifierNode, DrawModifierNode {
    private var lastX = Float.NaN
    private var lastY = Float.NaN

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (!coordinates.isAttached) return
        val pos = try {
            coordinates.positionInWindow()
        } catch (_: Exception) {
            null
        } ?: return
        if (pos.x != lastX || pos.y != lastY) {
            lastX = pos.x
            lastY = pos.y
            originBox[0] = pos
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
    }

    override fun onDetach() {
        lastX = Float.NaN
        lastY = Float.NaN
    }
}
