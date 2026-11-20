package restarhalf.stellar.schedule.ui.components.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.haooz.softglass.platform.GlassRuntimeShader
import com.haooz.softglass.platform.asComposeShader
import com.haooz.softglass.platform.createGlassRuntimeShader
import com.haooz.softglass.platform.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InteractiveHighlight(
    private val animationScope: CoroutineScope,
    private val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
    private val radiusScale: () -> Float = { 1f },
    private val radiusBaseDp: Dp? = null,
    private val fixedRadius: Boolean = false,
    private val drawFlatOverlay: Boolean = true,
    private val isDark: Boolean = false,
) {

    private companion object {
        const val SPRING_DAMPING_RATIO = 0.5f
        const val SPRING_STIFFNESS = 300f
        const val DARK_PEAK_ALPHA = 0.15f
        const val LIGHT_PEAK_ALPHA = 0.1f
    }

    private val visibilityThreshold = 0.001f
    private val offsetVisibilityThreshold = Offset(visibilityThreshold, visibilityThreshold)

    private val pressProgressAnimationSpec =
        spring(SPRING_DAMPING_RATIO, SPRING_STIFFNESS, visibilityThreshold)
    private val positionAnimationSpec =
        spring(SPRING_DAMPING_RATIO, SPRING_STIFFNESS, offsetVisibilityThreshold)

    private val pressProgressAnimation =
        Animatable(0f, visibilityThreshold)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, offsetVisibilityThreshold)

    private val dragOffsetAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, offsetVisibilityThreshold)

    private var startPosition = Offset.Zero
    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = dragOffsetAnimation.value

    private val shader =
        if (isRuntimeShaderSupported()) {
            createGlassRuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
            )
        } else {
            null
        }

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value
            if (progress > 0f) {
                val base = when {
                    radiusBaseDp == null -> size.minDimension
                    fixedRadius -> radiusBaseDp.toPx()
                    else -> minOf(size.minDimension, radiusBaseDp.toPx())
                }
                val radius = base * 1.5f * radiusScale()
                val pos = position(size, positionAnimation.value)
                val haloCenter = Offset(
                    pos.x.fastCoerceIn(0f, size.width),
                    pos.y.fastCoerceIn(0f, size.height)
                )
                val peak = (if (isDark) DARK_PEAK_ALPHA else LIGHT_PEAK_ALPHA) * progress
                if (drawFlatOverlay) {
                    drawRect(
                        Color.White.copy(0.08f * progress),
                        blendMode = BlendMode.Plus
                    )
                }
                if (shader != null) {
                    shader.apply {
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform("color", Color.White.copy(peak))
                        setFloatUniform("radius", radius)
                        setFloatUniform("position", haloCenter.x, haloCenter.y)
                    }
                    clipRect(
                        left = haloCenter.x - radius,
                        top = haloCenter.y - radius,
                        right = haloCenter.x + radius,
                        bottom = haloCenter.y + radius,
                    ) {
                        drawRect(
                            ShaderBrush(shader.asComposeShader()),
                            blendMode = BlendMode.Plus
                        )
                    }
                } else {
                    clipRect(
                        left = haloCenter.x - radius,
                        top = haloCenter.y - radius,
                        right = haloCenter.x + radius,
                        bottom = haloCenter.y + radius,
                    ) {
                        drawRect(
                            brush = Brush.radialGradient(
                                center = haloCenter,
                                radius = radius,
                                colors = listOf(
                                    Color.White.copy(peak),
                                    Color.White.copy(peak),
                                    Color.Transparent,
                                ),
                            ),
                            blendMode = BlendMode.Plus
                        )
                    }
                }
            }

            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                onDragStart = { down ->
                    startPosition = down.position
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                        launch { dragOffsetAnimation.snapTo(Offset.Zero) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { dragOffsetAnimation.animateTo(Offset.Zero, positionAnimationSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { dragOffsetAnimation.animateTo(Offset.Zero, positionAnimationSpec) }
                    }
                },
                observeConsumed = true,
            ) { change, _ ->
                animationScope.launch {
                    positionAnimation.snapTo(change.position)
                    dragOffsetAnimation.snapTo(change.position - startPosition)
                }
            }
        }

    val pressOnlyModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            awaitEachGesture {
                val down = awaitFirstDown()
                animationScope.launch {
                    pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec)
                }
                do {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                } while (event.changes.any { it.pressed })
                animationScope.launch {
                    pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec)
                }
            }
        }
}
