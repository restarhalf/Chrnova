package restarhalf.stellar.schedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur
import com.haooz.softglass.backdrop.effects.runtimeShaderEffect
import com.haooz.softglass.glass.SOFT_GLASS_LIVE_SHADER
import com.haooz.softglass.glass.SoftGlassBackdrop
import com.haooz.softglass.glass.SoftGlassStyle
import com.haooz.softglass.glass.applySoftGlass
import com.haooz.softglass.glass.glassBlurRadiusForSigma
import com.haooz.softglass.glass.prepareSoftGlassBackdrop
import com.haooz.softglass.glass.softGlass
import com.haooz.softglass.glass.wallpaperTexMap
import com.haooz.softglass.platform.asSoftwareBitmap
import com.haooz.softglass.platform.isSoftGlassShaderAvailable
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 柔光玻璃的绘制能力，供卡片调用。
 *
 * 需要这层抽象：卡片背景由平台画（AGSL / SkSL + 纹理采样），`commonMain` 引不到
 * `GlassRuntimeShader`，所以这里只暴露「给我一个 Modifier」。
 * shader 实例由 softglass 的 `SoftGlassProgramCache` 按尺寸 / 圆角共享。
 */
interface SoftGlassSurface {
    /**
     * @param origin 面板左上角的**窗口坐标**，绘制期求值。
     * @param radiusPx 圆角（px）；null = 用样式里的 `cornerRadius`（会被钳到短边一半）。
     * @param redraw 绘制期读一次的「位置信号」，让滚动 / 翻页时按帧失效。
     */
    fun modifier(
        density: Density,
        origin: () -> Offset,
        radiusPx: Float?,
        redraw: () -> Unit = {},
    ): Modifier
}

/**
 * 页面级柔光玻璃能力，按页共享。
 *
 * 只准备「壁纸纹理 + 坐标映射」，不做任何模糊 / 调色烘焙：
 * 拖模糊、拖壁纸、改任何外观参数都只重灌 uniform。无壁纸 / 平台没有 shader 时为 null，
 * 所有面板退回纯色表面。
 */
val LocalSoftGlassSurface = staticCompositionLocalOf<SoftGlassSurface?> { null }

/**
 * 卡片滚动 / 翻页的位置信号。
 *
 * 卡片的玻璃是在**卡片自己的 draw 里**画的，而 `verticalScroll` 与 `HorizontalPager`
 * 都用 `placeWithLayer` 平移内容：子树只是被整体搬走，不会重新绘制。不主动失效的话，
 * 玻璃纹理就跟着卡片一起平移（看起来像贴纸），松手/落页布局再次派发时才更新一帧。
 *
 * 在 `onDrawBehind` 里读这些 State，等于把这张卡的绘制订阅到滚动位置上：滚动 / 翻页 /
 * 回弹每变一个像素就按帧重画，`uOrigin` 才能跟手。
 */
val LocalGlassRedrawSignal = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * 边缘安全的实时柔光 shader：当偏移采样落入采样层外的透明空区（`s.a < 0.5`）时，
 * 平滑回退到当前像素中心色，避免反预乘产生硬边黑块。
 */
val EDGE_SAFE_SOFT_GLASS_LIVE_SHADER: String = buildEdgeSafeSoftGlassLiveShader()

private fun buildEdgeSafeSoftGlassLiveShader(): String {
    val contentBlockOld = """
        float3 sampleContent(float2 p) {
            return sampleContentRGBA(p).rgb;
        }

        // 大模糊底：控件脚下只有一张内容，用一对对角采样近似圆盘平均。
        // 内容本来就被 blur 过，两次采样足够代表大范围平均色，比原来 4 次省一半纹理读取。
        float3 contentBig(float2 p, float3 center) {
            float r = min(uSize.x, uSize.y) * 0.42;
            float3 sum = center;
            for (int i = 0; i < 2; i++) {
                float a = float(i) * 3.1415927 + 0.6;
                sum += sampleContent(p + float2(cos(a), sin(a)) * r);
            }
            return sum / 3.0;
        }
    """.trimIndent()
    val contentBlockNew = """
        float3 sampleContent(float2 p) {
            return sampleContentRGBA(p).rgb;
        }

        float3 sampleContentOr(float2 p, float3 fallback) {
            float4 s = sampleContentRGBA(p);
            return mix(fallback, s.rgb, smoothstep(0.05, 0.5, s.a));
        }

        float3 contentBig(float2 p, float3 center) {
            float r = min(uSize.x, uSize.y) * 0.42;
            float3 sum = center;
            for (int i = 0; i < 2; i++) {
                float a = float(i) * 3.1415927 + 0.6;
                sum += sampleContentOr(p + float2(cos(a), sin(a)) * r, center);
            }
            return sum / 3.0;
        }
    """.trimIndent()
    val refractOld = "refractionCol = sampleContent(fragCoord + refractDir.xy * refractThickness);"
    val refractNew = "refractionCol = sampleContentOr(fragCoord + refractDir.xy * refractThickness, centerSample.rgb);"
    val reflectOld =
        "        float3 reflectionCol = sampleContent(\n" +
            "            fragCoord + refractXY * (uReflectOffsetPx * (1.0 - w))\n" +
            "        );"
    val reflectNew =
        "        float3 reflectionCol = sampleContentOr(\n" +
            "            fragCoord + refractXY * (uReflectOffsetPx * (1.0 - w)),\n" +
            "            centerSample.rgb\n" +
            "        );"

    check(SOFT_GLASS_LIVE_SHADER.contains(contentBlockOld)) {
        "SOFT_GLASS_LIVE_SHADER missing contentBig block"
    }
    check(SOFT_GLASS_LIVE_SHADER.contains(refractOld)) {
        "SOFT_GLASS_LIVE_SHADER missing refractionCol line"
    }
    check(SOFT_GLASS_LIVE_SHADER.contains(reflectOld)) {
        "SOFT_GLASS_LIVE_SHADER missing reflectionCol block"
    }

    return SOFT_GLASS_LIVE_SHADER
        .replace(contentBlockOld, contentBlockNew)
        .replace(refractOld, refractNew)
        .replace(reflectOld, reflectNew)
}

/**
 * 实时录制版柔光：采样控件脚下的**真实内容**。
 *
 * 与 [SoftGlassSurface] 的差别只有采样源：静态路径采壁纸（更省、跨页可复用），
 * 浮层必须采「它下面此刻画了什么」，否则浮层一拖起来就成了纯色块。
 * 样式由调用方传入，两路共用同一份 [SoftGlassStyle]，参数不会漂移。
 *
 * @param shape 可见轮廓；实时路径靠它裁出玻璃形状（静态路径由 shader 自己处理圆角）。
 * @param radiusPx 圆角（px），只用于倒角带的光学距离。
 * @param redraw 绘制期读一次的位置信号，让 `graphicsLayer` 平移中的浮层逐帧重采。
 * @param blurSigmaPx 模糊量口径（屏幕 px）。
 */
@Composable
fun Modifier.softGlassBackdrop(
    backdrop: GlassBackdrop,
    shape: Shape,
    style: SoftGlassStyle,
    radiusPx: Float,
    redraw: () -> Unit = {},
    blurSigmaPx: Float? = null,
): Modifier {
    val styleState = rememberUpdatedState(style)
    val redrawState = rememberUpdatedState(redraw)
    val blurSigmaState = rememberUpdatedState(blurSigmaPx)
    // lambda 引用必须稳定：drawBackdrop 按引用比较 shape，每次重组新建会让节点重录采样层
    val shapeProvider: () -> Shape = remember(shape) { { shape } }
    val effects: GlassEffectScope.() -> Unit = remember(radiusPx) {
        {
            redrawState.value()
            val s = styleState.value
            // Mirror 让边缘不退化成一圈硬边。σ 口径（浮层对齐卡片）要先换回 BlurEffect 半径
            val sigma = blurSigmaState.value
            blur(
                if (sigma != null) glassBlurRadiusForSigma(sigma) else s.smallBlurRadius,
                TileMode.Mirror,
            )
            padding = 0f
            runtimeShaderEffect(
                // key 只认字符串：尺寸不同必须分开，否则同帧多张卡的 uniform 会互相覆盖
                key = "OverlaySoftGlass_${size.width.toInt()}x${size.height.toInt()}",
                shaderString = EDGE_SAFE_SOFT_GLASS_LIVE_SHADER,
                uniformShaderName = "content",
            ) {
                applySoftGlass(
                    style = s,
                    width = size.width,
                    height = size.height,
                    radiusPx = radiusPx,
                )
            }
        }
    }
    return drawBackdrop(
        backdrop = backdrop,
        shadow = null,
        shape = shapeProvider,
        effects = effects,
        onDrawSurface = null,
    )
}

/** 壁纸亮度的采样源重建延迟：亮度滑杆拖动中只跟手壁纸层，停手后卡片再补一帧。 */
private const val WALLPAPER_BRIGHTNESS_SETTLE_MS = 120L

/** 采样源最长边（与 `prepareSoftGlassBackdrop` 的 BASE_MAX_EDGE 一致），先缩小再调色省一次全尺寸拷贝。 */
private const val GLASS_SOURCE_MAX_EDGE = 1024

/**
 * 准备玻璃的采样源。
 *
 * ⚠ 只有 [wallpaperBitmap] 与 [wallpaperBrightness] 会触发重建（降采样 + 缩略图）。样式、
 * 缩放、偏移一律**不进任何 remember key**：它们分别只是 shader 的 uniform 与一个 2×2 仿射，
 * 改成 State 让绘制期去读，这样拖模糊 / 拖壁纸连卡片 modifier 都不会重建，更不会重烘位图。
 *
 * 壁纸亮度是用 ColorFilter 画出来的，而玻璃采的是原图；这里先把同一档亮度烘进采样源，
 * 否则卡片色调永远和壁纸差一档。拖动滑杆时不重建（每帧烘一座金字塔会卡），停手后再更新。
 *
 * @param wallpaperBrightness 已生效的壁纸亮度（-50..50），与壁纸层 `ColorFilter` 同口径
 * @param originTransform 绘制期求值的「窗口坐标 → 壁纸层逻辑坐标」修正
 */
@Composable
fun rememberSoftGlassSurface(
    wallpaperBitmap: ImageBitmap?,
    wallpaperScale: Float,
    wallpaperOffset: Offset,
    screenWidthPx: Float,
    screenHeightPx: Float,
    wallpaperBrightness: Float = 0f,
    wallpaperAlpha: Float = 1f,
    baseColor: Color = Color.Transparent,
    style: SoftGlassStyle = SoftGlassStyle(),
    originTransform: (Offset) -> Offset = { it },
): SoftGlassSurface? {
    val sourceBitmap = wallpaperBitmap
    if (sourceBitmap == null || !isSoftGlassShaderAvailable()) return null

    // 亮度 / 不透明度拖动中不停重烘金字塔会掉帧：先跟手壁纸层，停手后再生效。
    var settledBrightness by remember(sourceBitmap) { mutableStateOf(wallpaperBrightness) }
    var settledAlpha by remember(sourceBitmap) { mutableStateOf(wallpaperAlpha) }
    LaunchedEffect(sourceBitmap, wallpaperBrightness, wallpaperAlpha) {
        if (settledBrightness == wallpaperBrightness && settledAlpha == wallpaperAlpha) {
            return@LaunchedEffect
        }
        delay(WALLPAPER_BRIGHTNESS_SETTLE_MS)
        settledBrightness = wallpaperBrightness
        settledAlpha = wallpaperAlpha
    }
    val backdropBitmap = remember(sourceBitmap, settledBrightness, settledAlpha, baseColor) {
        tintWallpaperForGlass(sourceBitmap, settledBrightness, settledAlpha, baseColor)
    }
    val backdrop = remember(backdropBitmap) {
        prepareSoftGlassBackdrop(backdropBitmap)
    } ?: return null

    val styleState = remember { mutableStateOf(style) }
    SideEffect { styleState.value = style }

    val texMap = remember(backdrop, wallpaperScale, wallpaperOffset, screenWidthPx, screenHeightPx) {
        wallpaperTexMap(
            textureWidth = backdrop.levels[0].width,
            textureHeight = backdrop.levels[0].height,
            wallpaperScale = wallpaperScale,
            wallpaperOffset = wallpaperOffset,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
        )
    }
    // 映射做成 State：拖壁纸时只让绘制失效，不重建 surface，也不重建每张卡的 modifier。
    val texMapState = remember { mutableStateOf(texMap) }
    SideEffect { texMapState.value = texMap }

    return remember(backdrop, originTransform) {
        DefaultSoftGlassSurface(backdrop, styleState, texMapState, originTransform)
    }
}

/**
 * 把壁纸亮度、不透明度与底色烘进玻璃采样源。
 *
 * 亮度为 0 且不透明时直接复用原图；否则先缩到采样源分辨率再按与壁纸层一致的
 * 底色、透明度与乘性 ColorFilter 绘制，避免多拷一份原尺寸位图。
 */
private fun tintWallpaperForGlass(
    bitmap: ImageBitmap,
    brightness: Float,
    alpha: Float = 1f,
    baseColor: Color = Color.Transparent,
): ImageBitmap {
    val clampedAlpha = alpha.coerceIn(0f, 1f)
    val gain = (1f + brightness / 50f).coerceIn(0f, 2f)
    if (gain == 1f && clampedAlpha >= 0.999f) return bitmap

    // 硬件位图不能画进软件 Canvas
    val source = bitmap.asSoftwareBitmap()
    val longest = max(source.width, source.height)
    val scale =
        if (longest > GLASS_SOURCE_MAX_EDGE) GLASS_SOURCE_MAX_EDGE.toFloat() / longest else 1f
    val width = (source.width * scale).roundToInt().coerceAtLeast(1)
    val height = (source.height * scale).roundToInt().coerceAtLeast(1)

    val out = ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
    val canvas = Canvas(out)
    if (clampedAlpha < 0.999f && baseColor.alpha > 0f) {
        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = width.toFloat(),
            bottom = height.toFloat(),
            paint = Paint().apply { color = baseColor },
        )
    }
    canvas.drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = Paint().apply {
            filterQuality = FilterQuality.Medium
            this.alpha = clampedAlpha
            if (gain != 1f) {
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            gain, 0f, 0f, 0f, 0f,
                            0f, gain, 0f, 0f, 0f,
                            0f, 0f, gain, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
            }
        },
    )
    return out
}

private class DefaultSoftGlassSurface(
    private val backdrop: SoftGlassBackdrop,
    private val styleState: State<SoftGlassStyle>,
    private val texMapState: State<FloatArray>,
    private val originTransform: (Offset) -> Offset,
) : SoftGlassSurface {
    override fun modifier(
        density: Density,
        origin: () -> Offset,
        radiusPx: Float?,
        redraw: () -> Unit,
    ): Modifier = Modifier.softGlass(
        backdrop = backdrop,
        styleProvider = { styleState.value },
        density = density,
        // positionInWindow() 含祖先 graphicsLayer 变换，先还原到壁纸层的逻辑坐标系
        origin = { originTransform(origin()) },
        texMapProvider = { texMapState.value },
        radiusPx = radiusPx,
        redraw = redraw,
    )
}
