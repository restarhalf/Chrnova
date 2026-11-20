package restarhalf.stellar.schedule.ui.components.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur
import com.haooz.softglass.platform.isRenderEffectSupported
import restarhalf.stellar.schedule.ui.model.WeekHeaderUi
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import restarhalf.stellar.schedule.ui.modifier.softGlassRim
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.CollapsibleTopAppBar
import top.yukonga.miuix.kmp.layout.CollapsibleTopAppBarDefaults
import top.yukonga.miuix.kmp.layout.ProgressiveBlurTopBar
import top.yukonga.miuix.kmp.layout.SharedScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private val DayRowHeight = 44.dp

private val SectionLabelWidth = 36.dp

private val ContentHorizontalPadding = 8.dp

fun scheduleContentTopPadding(statusBarHeight: Dp): Dp =
    statusBarHeight + CollapsibleTopAppBarDefaults.CollapsedHeight + DayRowHeight + 2.dp

@Composable
fun ScheduleTopBar(
    title: String,
    weekHeaderUi: WeekHeaderUi,
    backdrop: GlassBackdrop?,
    scrollBehavior: SharedScrollBehavior,
    modifier: Modifier = Modifier,
    onMoreSlotCoordinates: (LayoutCoordinates) -> Unit = {},
    onMoreMaterial: (Float) -> Unit = {},
    onMoreExpand: () -> Unit = {},
    actions: @Composable RowScope.(backdropAlpha: Float, shadowAlpha: Float) -> Unit,
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barHeight = if (statusBarHeight > 0.dp) 120.dp + statusBarHeight else 160.dp
    val materialAlpha = rememberTopBarMaterialAlpha(scrollBehavior)

    val content: @Composable BoxScope.() -> Unit = {
        Box {
            Box(modifier = Modifier.zIndex(1f)) {
                CollapsibleTopAppBar(
                    title = title,
                    showLargeTitle = false,
                    scrollBehavior = scrollBehavior,
                    onAlphaChanged = { backdropAlpha, _ -> onMoreMaterial(backdropAlpha) },
                    endAction = { backdropAlpha, shadowAlpha ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            actions(backdropAlpha, shadowAlpha)
                            Spacer(
                                modifier =
                                    Modifier
                                        .size(42.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = onMoreExpand,
                                        )
                                        .semantics { contentDescription = "更多" }
                                        .onGloballyPositioned { onMoreSlotCoordinates(it) },
                            )
                        }
                    },
                )
            }

            DayOfWeekRow(
                ui = weekHeaderUi,
                backdrop = backdrop,
                modifier = Modifier.dayOfWeekTopPadding(statusBarHeight, scrollBehavior),
            )
        }
    }

    if (backdrop != null) {
        ProgressiveBlurTopBar(
            backdrop = backdrop,
            modifier = modifier.fillMaxWidth(),
            height = barHeight,
            maskAlpha = materialAlpha,
            content = content,
        )
    } else {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface),
            content = content,
        )
    }
}

@Composable
private fun DayOfWeekRow(
    ui: WeekHeaderUi,
    backdrop: GlassBackdrop?,
    modifier: Modifier,
) {
    val accent = MiuixTheme.colorScheme.primary
    val weekdayColor = MiuixTheme.colorScheme.onSurface
    val dateColor = MiuixTheme.colorScheme.onSurfaceVariantActions
    val glassEffectsOn = AppMaterialSettings.glassEffectsEnabled()
    val glassBlur = AppMaterialSettings.glassBlurDp()
    val isLightTheme = MiuixTheme.colorScheme.background.luminance() >= 0.5f
    val pillColor = accent.copy(alpha = AppMaterialSettings.solidSurfaceAlpha(0.7f))
    // 这颗「今天」日期胶囊直径只有 20–26dp，而 SoftGlassStyle.edgeWidth 是 60px 口径：
    // 接 applySoftGlass 会让整颗丸落在倒角带里（inner 区为零），变成扭曲光斑而非小玻璃珠。
    // 因此只保留模糊 + 半透明 accent 底，不接柔光 shader。
    val pillEffects: GlassEffectScope.() -> Unit =
        remember(glassEffectsOn, glassBlur) {
            {
                if (glassEffectsOn) {
                    blur(glassBlur.dp.toPx(), TileMode.Mirror)
                    padding = 0f
                }
            }
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DayRowHeight)
                .padding(horizontal = ContentHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(SectionLabelWidth))

        ui.days.zip(ui.dates).forEachIndexed { index, (day, date) ->
            DayCell(
                day = day,
                date = date,
                isToday = ui.todayIndex == index,
                accent = accent,
                backdrop = backdrop,
                pillColor = pillColor,
                pillEffects = pillEffects,
                isLightTheme = isLightTheme,
                weekdayColor = weekdayColor,
                dateColor = dateColor,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun DayCell(
    day: String,
    date: String,
    isToday: Boolean,
    accent: Color,
    backdrop: GlassBackdrop?,
    pillColor: Color,
    pillEffects: GlassEffectScope.() -> Unit,
    isLightTheme: Boolean,
    weekdayColor: Color,
    dateColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = day,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isToday) accent else weekdayColor,
            maxLines = 1,
        )
        DayDateLabel(
            date = date,
            isToday = isToday,
            backdrop = backdrop,
            pillColor = pillColor,
            pillEffects = pillEffects,
            isLightTheme = isLightTheme,
            dateColor = dateColor,
        )
    }
}

private val DayDateBlurRadius = 8.dp

@Composable
private fun DayDateLabel(
    date: String,
    isToday: Boolean,
    backdrop: GlassBackdrop?,
    pillColor: Color,
    pillEffects: GlassEffectScope.() -> Unit,
    isLightTheme: Boolean,
    dateColor: Color,
) {
    val supported = remember { isRenderEffectSupported() }
    val blur = remember { Animatable(0f) }
    val isFirstKey = remember { booleanArrayOf(true) }
    val key = remember(date, isToday) { date to isToday }
    LaunchedEffect(key) {
        if (isFirstKey[0]) {
            isFirstKey[0] = false
            return@LaunchedEffect
        }
        if (!supported) return@LaunchedEffect
        blur.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
        blur.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
    }
    val blurRadiusPx = with(LocalDensity.current) { DayDateBlurRadius.toPx() }

    Box(
        modifier =
            Modifier.graphicsLayer {
                val radius = blur.value * blurRadiusPx
                renderEffect =
                    if (supported && radius > 0.01f) {
                        BlurEffect(radius, radius, TileMode.Decal)
                    } else {
                        null
                    }
            },
    ) {
        AnimatedContent(
            targetState = key,
            transitionSpec = {
                (
                    fadeIn(tween(320, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(320, easing = FastOutSlowInEasing))
                    )
                    .using(SizeTransform(clip = false))
            },
            contentAlignment = Alignment.Center,
            label = "dayDate",
        ) { (shownDate, shownToday) ->
            Box(contentAlignment = Alignment.Center) {
                if (shownToday) {
                    val pillShape: () -> androidx.compose.ui.graphics.Shape =
                        remember { { CircleShape } }
                    val pillSurface: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit =
                        remember(pillColor) { { drawRect(pillColor) } }
                    val pillModifier =
                        if (backdrop != null) {
                            Modifier
                                .graphicsLayer {}
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = pillShape,
                                    effects = pillEffects,
                                    shadow = null,
                                    onDrawSurface = pillSurface,
                                )
                                .softGlassRim(CircleShape, isLightTheme)
                        } else {
                            Modifier.background(pillColor, CircleShape)
                        }
                    Box(modifier = Modifier.matchParentSize().then(pillModifier))
                }
                Text(
                    text = shownDate,
                    style = MiuixTheme.textStyles.footnote2,
                    fontWeight = if (shownToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (shownToday) Color.White else dateColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun Modifier.dayOfWeekTopPadding(
    statusBarHeight: Dp,
    scrollBehavior: SharedScrollBehavior,
): Modifier =
    layout { measurable, constraints ->
        val statusBarPx = statusBarHeight.roundToPx()
        val reportedPx = scrollBehavior.currentHeightPx
        val barPx =
            if (reportedPx > 0f) {
                reportedPx.roundToInt()
            } else {
                CollapsibleTopAppBarDefaults.CollapsedHeight.roundToPx()
            }
        val gapPx = if (statusBarPx > 0) 0 else DayRowHeight.roundToPx()
        val top = statusBarPx + barPx + gapPx
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height + top) {
            placeable.place(0, top)
        }
    }
