package restarhalf.stellar.schedule.ui.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.backdrops.layerBackdrop
import com.haooz.softglass.backdrop.backdrops.rememberCombinedBackdrop
import com.haooz.softglass.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.ui.components.LocalWallpaperBackdrop
import restarhalf.stellar.schedule.ui.components.topbar.AppMoreMenuItem
import restarhalf.stellar.schedule.ui.components.topbar.LiquidGlassCircle
import restarhalf.stellar.schedule.ui.components.topbar.LiquidTopBarButton
import restarhalf.stellar.schedule.ui.components.topbar.MorePopupMenu
import restarhalf.stellar.schedule.ui.icons.Back
import top.yukonga.miuix.kmp.basic.LocalPullToRefreshTopInset
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.layout.CollapsibleTopAppBar
import top.yukonga.miuix.kmp.layout.CollapsibleTopAppBarDefaults
import top.yukonga.miuix.kmp.layout.ProgressiveBlurTopBar
import top.yukonga.miuix.kmp.layout.SharedScrollBehavior
import top.yukonga.miuix.kmp.layout.rememberSharedScrollBehavior
import top.yukonga.miuix.kmp.layout.rememberTopBarMaterialState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.LocalOverScrollState
import top.yukonga.miuix.kmp.utils.OverScrollState
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

fun Modifier.pageScrollModifiers(
    showTopAppBar: Boolean = true,
    scrollBehavior: ScrollBehavior,
): Modifier =
    this
        .then(
            if (showTopAppBar) {
                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            } else {
                Modifier
            },
        )
        .scrollEndHaptic()
        .overScrollVertical()
        .fillMaxHeight()

fun Modifier.pageScrollModifiers(
    scrollBehavior: SharedScrollBehavior,
): Modifier =
    this
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .scrollEndHaptic()
        .overScrollVertical()
        .fillMaxHeight()

@Composable
fun appPageContentPadding(
    innerPadding: PaddingValues,
    outerPadding: PaddingValues,
    extraTop: Dp = 0.dp,
    extraStart: Dp = 0.dp,
    extraEnd: Dp = 0.dp,
): PaddingValues {
    val topPadding = innerPadding.calculateTopPadding() + extraTop
    // 底部取页面自身 Scaffold（含 bottomBar 高度）与外层导航栏留白的较大者：
    // 有底部按钮的页面（如提醒设置的测试按钮）bottomBar 高于导航栏，
    // 只算外层留白会让列表最后一项滚到按钮底下被遮住。
    val bottomPadding =
        maxOf(
            outerPadding.calculateBottomPadding(),
            innerPadding.calculateBottomPadding(),
        ) + 12.dp

    return remember(topPadding, bottomPadding, extraStart, extraEnd) {
        PaddingValues(
            top = topPadding,
            start = extraStart,
            end = extraEnd,
            bottom = bottomPadding,
        )
    }
}

@Composable
fun rememberAppPageScrollBehavior(): SharedScrollBehavior = rememberSharedScrollBehavior()

@Composable
fun AppPageScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    moreMenuItems: List<AppMoreMenuItem> = emptyList(),
    showLargeTitle: Boolean = true,
    showSmallTitle: Boolean? = null,
    showShadow: Boolean? = null,
    topBarExtra: @Composable () -> Unit = {},
    popupHost: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = Color.Transparent,
    content: @Composable (scrollBehavior: SharedScrollBehavior, innerPadding: PaddingValues) -> Unit,
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var extraHeightPx by remember { mutableIntStateOf(0) }
    val extraHeight = with(density) { extraHeightPx.toDp() }

    val wallpaperBackdrop = LocalWallpaperBackdrop.current
    val pageBaseColor =
        if (containerColor != Color.Transparent) containerColor else MiuixTheme.colorScheme.surface
    // 使用默认 DefaultOnDraw（contentOnlyCapture = true），背景色放到 layerBackdrop 右侧的 .background(...)，
    // 避免整页内容画两遍；并配合 recordKey + mustRecord，静止时跳过重复录帧与 contentVersion 递增。
    val pageBackdrop = rememberLayerBackdrop()
    val topBarBackdrop =
        if (wallpaperBackdrop != null) {
            rememberCombinedBackdrop(wallpaperBackdrop, pageBackdrop)
        } else {
            pageBackdrop
        }
    val scrollBehavior = rememberSharedScrollBehavior()
    val pageDirtyUntil = remember { arrayOfNulls<TimeMark>(1) }
    val pageRecordKey =
        remember(title, pageBaseColor, wallpaperBackdrop != null, extraHeightPx) {
            listOf(title, pageBaseColor, wallpaperBackdrop != null, extraHeightPx)
        }
    val pageMustRecord: () -> Boolean =
        remember(scrollBehavior, pageDirtyUntil) {
            var lastHeightOffset = Float.NaN
            var lastContentOffset = Float.NaN
            var lastPostCollapse = Float.NaN
            {
                val h = scrollBehavior.state.heightOffset
                val c = scrollBehavior.state.contentOffset
                val p = scrollBehavior.postCollapseScrollOffset
                val scrollMoved =
                    h != lastHeightOffset || c != lastContentOffset || p != lastPostCollapse
                lastHeightOffset = h
                lastContentOffset = c
                lastPostCollapse = p
                scrollMoved || (pageDirtyUntil[0]?.hasPassedNow() == false)
            }
        }
    val pageOverScrollState = remember { OverScrollState() }
    var showMoreMenu by remember { mutableStateOf(false) }
    val moreMenuFraction = remember { Animatable(0f) }
    val moreSlotCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val moreSampleBackdrop = rememberLayerBackdrop()
    val moreMenuBackdrop = rememberCombinedBackdrop(topBarBackdrop, moreSampleBackdrop)
    var topBarSampled by remember { mutableStateOf(false) }
    if (showMoreMenu && !topBarSampled) topBarSampled = true
    LaunchedEffect(showMoreMenu) {
        if (showMoreMenu) return@LaunchedEffect
        snapshotFlow { moreMenuFraction.value }.first { it <= 0.5f }
        topBarSampled = false
    }
    val moreSampleRecordKey = remember(pageRecordKey, topBarSampled, showMoreMenu) {
        listOf(pageRecordKey, topBarSampled, showMoreMenu)
    }
    val moreSampleMustRecord: () -> Boolean = remember(pageMustRecord, moreMenuFraction) {
        {
            moreMenuFraction.isRunning || pageMustRecord()
        }
    }
    val tintColor =
        if (containerColor == Color.Transparent) {
            MiuixTheme.colorScheme.surface
        } else {
            containerColor
        }

    // 每页独立 OverScrollState + 唯一 TopBarMaterialState，避免跨页残留越界量并让渐进顶栏与按钮玻璃统一听命于 showShadow。
    CompositionLocalProvider(LocalOverScrollState provides pageOverScrollState) {
        val materialState =
            if (showShadow != null) {
                rememberTopBarMaterialState(showShadow)
            } else {
                rememberTopBarMaterialState(scrollBehavior)
            }
        val materialAlpha = materialState.alpha

        Scaffold(
            containerColor = containerColor,
            topBar = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (topBarSampled) {
                                    Modifier.layerBackdrop(
                                        backdrop = moreSampleBackdrop,
                                        recordKey = moreSampleRecordKey,
                                        mustRecord = moreSampleMustRecord,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    ProgressiveBlurTopBar(
                        backdrop = topBarBackdrop,
                        modifier = Modifier.fillMaxWidth(),
                        height = statusBarHeight + CollapsibleTopAppBarDefaults.CollapsedHeight + extraHeight + 28.dp,
                        tintColor = tintColor,
                        maskAlpha = materialAlpha,
                    ) {
                        Column {
                            CollapsibleTopAppBar(
                                title = title,
                                scrollBehavior = scrollBehavior,
                                materialState = materialState,
                                showLargeTitle = showLargeTitle,
                                showSmallTitle = showSmallTitle,
                                showShadow = showShadow,
                                contentPadding = {},
                                startAction =
                                    if (onBack != null) {
                                        { backdropAlpha, shadowAlpha ->
                                            LiquidTopBarButton(
                                                onClick = onBack,
                                                backdrop = topBarBackdrop,
                                                icon = Back,
                                                contentDescription = "返回",
                                                iconSize = 22.dp,
                                                backdropAlpha = backdropAlpha,
                                                shadowAlpha = shadowAlpha,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                endAction =
                                    if (actions != null || moreMenuItems.isNotEmpty()) {
                                        { backdropAlpha, shadowAlpha ->
                                            CompositionLocalProvider(
                                                LocalTopBarBackdrop provides topBarBackdrop,
                                                LocalTopBarBackdropAlpha provides backdropAlpha,
                                                LocalTopBarShadowAlpha provides shadowAlpha,
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    if (actions != null) actions()
                                                    if (moreMenuItems.isNotEmpty()) {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .size(42.dp)
                                                                    .onGloballyPositioned {
                                                                        moreSlotCoordinates.value = it
                                                                    },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        null
                                    },
                            )
                            CompositionLocalProvider(LocalTopBarBackdrop provides topBarBackdrop) {
                                Box(Modifier.onSizeChanged { extraHeightPx = it.height }) {
                                    topBarExtra()
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = bottomBar,
            popupHost = {
                if (moreMenuItems.isNotEmpty()) {
                    MorePopupMenu(
                        showMorePopup = showMoreMenu,
                        onMorePopupDismiss = { showMoreMenu = false },
                        onMorePopupExpand = { showMoreMenu = true },
                        items = moreMenuItems,
                        morePopupFraction = moreMenuFraction,
                        moreSlotCoordinates = moreSlotCoordinates,
                        liquidGlassBackdrop = moreMenuBackdrop,
                        materialAlpha = materialAlpha,
                    )
                }
                popupHost()
            },
        ) { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(pageDirtyUntil) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                    pageDirtyUntil[0] = TimeSource.Monotonic.markNow() + 550.milliseconds
                                }
                            }
                        }
                        .layerBackdrop(
                            backdrop = pageBackdrop,
                            recordKey = pageRecordKey,
                            mustRecord = pageMustRecord,
                        )
                        .then(
                            if (wallpaperBackdrop == null) {
                                Modifier.background(pageBaseColor)
                            } else {
                                Modifier
                            },
                        ),
            ) {
                CompositionLocalProvider(
                    LocalPullToRefreshTopInset provides
                        (statusBarHeight + extraHeight + CollapsibleTopAppBarDefaults.CollapsedHeight),
                ) {
                    content(
                        scrollBehavior,
                        PaddingValues(
                            top = statusBarHeight + extraHeight + CollapsibleTopAppBarDefaults.CollapsedHeight,
                            bottom = paddingValues.calculateBottomPadding(),
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun AppPageTopBar(
    title: String,
    scrollBehavior: SharedScrollBehavior,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    color: Color = Color.Transparent,
    titleColor: Color = Color.Unspecified,
) {
    val backdrop = LocalWallpaperBackdrop.current
    val resolvedTitleColor =
        if (titleColor == Color.Unspecified) {
            MiuixTheme.colorScheme.onSurface
        } else {
            titleColor
        }
    val content: @Composable BoxScope.() -> Unit = {
        CollapsibleTopAppBar(
            title = title,
            showLargeTitle = false,
            scrollBehavior = scrollBehavior,
            titleColor = resolvedTitleColor,
            startAction = { _, _ ->
                TopBarGlassIcon(backdrop = backdrop) { navigationIcon() }
            },
            endAction = { _, _ ->
                CompositionLocalProvider(LocalTopBarBackdrop provides backdrop) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            },
        )
    }

    if (backdrop != null) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        ProgressiveBlurTopBar(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            height = statusBarHeight + CollapsibleTopAppBarDefaults.CollapsedHeight,
            tintColor =
                if (color == Color.Transparent) {
                    MiuixTheme.colorScheme.surface
                } else {
                    color
                },
            content = content,
        )
    } else {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(color),
            content = content,
        )
    }
}

val LocalTopBarBackdrop = compositionLocalOf<GlassBackdrop?> { null }
val LocalTopBarBackdropAlpha = compositionLocalOf { 1f }
val LocalTopBarShadowAlpha = compositionLocalOf { 1f }

@Composable
fun AppTopBarActionContainer(content: @Composable () -> Unit) {
    TopBarGlassIcon(
        backdrop = LocalTopBarBackdrop.current,
        backdropAlpha = LocalTopBarBackdropAlpha.current,
        shadowAlpha = LocalTopBarShadowAlpha.current,
        content = content,
    )
}

private val TopBarGlassSize = 42.dp

@Composable
private fun TopBarGlassIcon(
    backdrop: GlassBackdrop?,
    backdropAlpha: Float = 1f,
    shadowAlpha: Float = 1f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    SubcomposeLayout { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val contentPlaceables = subcompose("content", content).map { it.measure(looseConstraints) }
        val contentWidth = contentPlaceables.maxOfOrNull { it.width } ?: 0
        val contentHeight = contentPlaceables.maxOfOrNull { it.height } ?: 0
        if (contentWidth == 0 || contentHeight == 0) {
            layout(0, 0) {}
        } else {
            val glassSize = with(density) { TopBarGlassSize.roundToPx() }
            val glassPlaceables =
                subcompose("glass") {
                    LiquidGlassCircle(
                        backdrop = backdrop,
                        size = TopBarGlassSize,
                        backdropAlpha = backdropAlpha,
                        shadowAlpha = shadowAlpha,
                    )
                }.map { it.measure(Constraints.fixed(glassSize, glassSize)) }
            layout(contentWidth, contentHeight) {
                glassPlaceables.firstOrNull()?.place(
                    x = (contentWidth - glassSize) / 2,
                    y = (contentHeight - glassSize) / 2,
                )
                contentPlaceables.forEach { it.place(0, 0) }
            }
        }
    }
}

@Composable
fun AppPageTopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    color: Color = Color.Transparent,
    titleColor: Color = Color.Unspecified,
) {
    SmallTopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        defaultWindowInsetsPadding = false,
        navigationIcon = navigationIcon,
        actions = actions,
        color = color,
        titleColor = titleColor,
    )
}
