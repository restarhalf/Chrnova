package restarhalf.stellar.schedule.pictureselector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import com.haooz.softglass.backdrop.GlassBackdrop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.CapsuleSegmentedControl
import restarhalf.stellar.schedule.ui.components.SheetGlassButton
import restarhalf.stellar.schedule.ui.components.topbar.rememberTopBarMaterialAlpha
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Close
import restarhalf.stellar.schedule.ui.image.toAsyncImageModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.layout.ProgressiveBlurTopBar
import top.yukonga.miuix.kmp.layout.rememberSharedScrollBehavior
import top.yukonga.miuix.kmp.overlay.BlurBottomSheet
import top.yukonga.miuix.kmp.overlay.LocalSheetContentBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.foundation.lazy.items as lazyItems

private val PhotoShape = RoundedCornerShape(10.dp)
private val AlbumCoverShape = RoundedCornerShape(14.dp)
private val AlbumCardSize = 112.dp
private val AlbumDetailHeaderHeight = 76.dp


private data class SelectorPage(
    val tab: PictureSelectorTab,
    val album: MediaAlbum?,
) {
    val inAlbumDetail: Boolean get() = tab == PictureSelectorTab.Albums && album != null
}
private val EaseOutFast = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val EaseInFast = CubicBezierEasing(0.4f, 0f, 1f, 1f)

private const val PushDurationMs = 200
private const val PopDurationMs = 180
private const val TabDurationMs = 160

@Composable
fun PictureSelectorSheet(
    show: Boolean,
    hasPermission: Boolean,
    permissionSummary: String,
    onRequestPermission: () -> Unit,
    outputWidthPx: Int?,
    outputHeightPx: Int?,
    onDismissRequest: () -> Unit,
    onPicked: (String) -> Unit,
    port: PictureSelectorPort,
) {
    if (!show) return

    val state = rememberPictureSelectorState(port)
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)

    LaunchedEffect(show, hasPermission) {
        if (!show) {
            state.resetTransientState()
            return@LaunchedEffect
        }
        if (hasPermission) {
            state.refresh()
        }
    }

    val goBackOrDismiss: () -> Unit = {
        when {
            state.cropTarget != null -> state.closeCropper()
            state.selectedTab == PictureSelectorTab.Albums && state.currentAlbum != null ->
                state.backToAlbumList()

            else -> {
                state.resetTransientState()
                currentOnDismiss()
            }
        }
    }

    val cropTarget = state.cropTarget
    if (cropTarget != null && outputWidthPx != null && outputHeightPx != null) {
        val cropBackState = rememberNavigationEventState(NavigationEventInfo.None)
        NavigationBackHandler(
            state = cropBackState,
            isBackEnabled = true,
            onBackCompleted = { state.closeCropper() },
        )
        CropScreen(
            imageUri = cropTarget.contentUri,
            outputWidthPx = outputWidthPx,
            outputHeightPx = outputHeightPx,
            port = port,
            onCancel = { state.closeCropper() },
            onCropped = { croppedUri ->
                state.closeCropper()
                currentOnPicked(croppedUri)
            },
        )
        return
    }

    val onImageClick: (MediaImage) -> Unit =
        if (outputWidthPx == null || outputHeightPx == null) {
            { image -> currentOnPicked(image.contentUri) }
        } else {
            state::openCropper
        }

    val page = SelectorPage(tab = state.selectedTab, album = state.currentAlbum)
    val inAlbumDetail = page.inAlbumDetail
    // 相册详情页盖在网格之上，需要一层实色挡住滑出的内容，取弹窗自身底色贴近玻璃卡片
    val detailBackground = BottomSheetDefaults.backgroundColor()

    // 抄系统相册的底层结构：网格铺满整张弹窗、从浮动分段标签下面滚过，
    // 滚过时渐隐顶栏（ProgressiveBlurTopBar）按 app 既有套路弹簧浮现；
    // 不再用 Spacer 把内容顶下去、底部留死区。
    val headerZone = 138.dp
    val topBarHeight = if (inAlbumDetail) AlbumDetailHeaderHeight else headerZone
    val scrollBehavior = rememberSharedScrollBehavior()
    val materialAlpha = rememberTopBarMaterialAlpha(scrollBehavior)
    val listScroll =
        Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
            .overScrollVertical()
    val pagerState =
        rememberPagerState(
            initialPage = if (state.selectedTab == PictureSelectorTab.All) 0 else 1,
            pageCount = { 2 },
        )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { pageIndex ->
            val targetTab = if (pageIndex == 0) PictureSelectorTab.All else PictureSelectorTab.Albums
            if (state.selectedTab != targetTab) {
                state.selectTab(targetTab)
            }
        }
    }
    LaunchedEffect(state.selectedTab) {
        val targetPage = if (state.selectedTab == PictureSelectorTab.All) 0 else 1
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    BlurBottomSheet(
        show = true,
        title = if (inAlbumDetail) page.album?.bucketName ?: "选择图片" else "选择图片",
        dimBackground = true,
        onDismissRequest = goBackOrDismiss,
        startAction = {
            SheetGlassButton(
                icon = if (inAlbumDetail) Back else Close,
                contentDescription = if (inAlbumDetail) "返回" else "关闭",
                onClick = goBackOrDismiss,
                modifier = Modifier.padding(start = 18.dp),
            )
        },
        // 渐隐顶栏 + 浮动分段标签放在外壳悬浮层：ProgressiveBlurTopBar 要采样
        // LocalSheetContentBackdrop（内容录制层），只能在录制层之外画，否则图层自包含闪退。
        headerOverlay = {
            val sheetContentBackdrop = LocalSheetContentBackdrop.current
            if (sheetContentBackdrop != null) {
                ProgressiveBlurTopBar(
                    backdrop = sheetContentBackdrop,
                    modifier = Modifier.align(Alignment.TopCenter),
                    height = topBarHeight,
                    maskAlpha = materialAlpha,
                ) { }
            }
            AnimatedVisibility(
                visible = !inAlbumDetail,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 78.dp, start = 12.dp, end = 12.dp),
                enter = fadeIn(tween(TabDurationMs)),
                exit = fadeOut(tween(TabDurationMs / 2)),
            ) {
                SelectorTabs(
                    selectedIndex = pagerState.currentPage,
                    onSelectIndex = { index ->
                        val targetTab =
                            if (index == 0) PictureSelectorTab.All else PictureSelectorTab.Albums
                        state.selectTab(targetTab)
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    // 悬浮在内容录制层之外，采样弹窗里滚动的内容本身（抄系统相册标签糊住下方照片）
                    backdrop = sheetContentBackdrop,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        // 相册详情页用内层返回拦截，避免预测性返回手势把整个弹窗下拉至半屏
        val detailBackState = rememberNavigationEventState(NavigationEventInfo.None)
        NavigationBackHandler(
            state = detailBackState,
            isBackEnabled = inAlbumDetail,
            onBackCompleted = { state.backToAlbumList() },
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (!hasPermission) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().then(listScroll),
                    contentPadding = PaddingValues(top = headerZone, start = 16.dp, end = 16.dp, bottom = 24.dp),
                ) {
                    item(key = "permission") {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            PermissionContent(
                                summary = permissionSummary,
                                onRequestPermission = onRequestPermission,
                            )
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !inAlbumDetail,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    if (pageIndex == 0) {
                        ImageGrid(
                            images = state.allImages,
                            isRefreshing = state.isRefreshing,
                            isLoadingMore = state.isLoadingMore,
                            onReachListEnd = { scope.launch { state.loadMoreIfNeeded() } },
                            onImageClick = onImageClick,
                            modifier = Modifier.fillMaxSize().then(listScroll),
                            contentTopPadding = headerZone,
                            port = port,
                        )
                    } else {
                        AlbumList(
                            albums = state.albums,
                            isRefreshing = state.isRefreshing,
                            onAlbumClick = { album ->
                                scope.launch { state.openAlbum(album) }
                            },
                            modifier = Modifier.fillMaxSize().then(listScroll),
                            contentTopPadding = headerZone,
                            port = port,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = inAlbumDetail,
                    modifier = Modifier.fillMaxSize(),
                    enter = slideInHorizontally(
                        animationSpec = tween(PushDurationMs, easing = EaseOutFast),
                    ) { it },
                    exit = slideOutHorizontally(
                        animationSpec = tween(PopDurationMs, easing = EaseInFast),
                    ) { it },
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(detailBackground),
                    ) {
                        ImageGrid(
                            images = state.currentAlbumImages,
                            isRefreshing = state.isRefreshing,
                            isLoadingMore = state.isLoadingMore,
                            onReachListEnd = { scope.launch { state.loadMoreIfNeeded() } },
                            onImageClick = onImageClick,
                            modifier = Modifier.fillMaxSize().then(listScroll),
                            contentTopPadding = AlbumDetailHeaderHeight,
                            port = port,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionContent(
    summary: String,
    onRequestPermission: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = summary, color = colors.onSurfaceVariantSummary)
            Button(
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = onRequestPermission,
            ) {
                Text(text = "授予权限", color = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun SelectorTabs(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    backdrop: GlassBackdrop?,
    modifier: Modifier = Modifier,
) {
    CapsuleSegmentedControl(
        options = listOf("全部图片", "相册"),
        selectedIndex = selectedIndex,
        onSelect = onSelectIndex,
        // 调用方在弹窗外壳悬浮层（内容录制层之外）传入 LocalSheetContentBackdrop，
        // 标签玻璃糊住弹窗里滚动的内容；在那里采样不会图层自包含闪退。
        backdrop = backdrop,
        modifier = modifier,
    )
}

@Composable
private fun AlbumList(
    albums: List<MediaAlbum>,
    isRefreshing: Boolean,
    onAlbumClick: (MediaAlbum) -> Unit,
    modifier: Modifier = Modifier,
    contentTopPadding: Dp = 0.dp,
    port: PictureSelectorPort,
) {
    when {
        isRefreshing && albums.isEmpty() ->
            PlaceholderText("正在读取相册...", modifier, contentTopPadding)
        albums.isEmpty() ->
            PlaceholderText("没有找到图片", modifier, contentTopPadding)
        else -> {
            // 仿系统相册页：「相册」加粗标题 + 横向封面卡片列表，相册名置于预览图下方
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = contentTopPadding + 14.dp, bottom = 24.dp),
            ) {
                item(key = "albums_section") {
                    AlbumSection(
                        albums = albums,
                        onAlbumClick = onAlbumClick,
                        port = port,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumSection(
    albums: List<MediaAlbum>,
    onAlbumClick: (MediaAlbum) -> Unit,
    port: PictureSelectorPort,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "相册",
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            lazyItems(albums, key = { it.bucketId }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) },
                    port = port,
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: MediaAlbum,
    onClick: () -> Unit,
    port: PictureSelectorPort,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier =
            Modifier
                .width(AlbumCardSize)
                .clip(AlbumCoverShape)
                .clickable(onClick = onClick)
                .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SelectorThumbnail(
            uri = album.coverUri,
            contentDescription = album.bucketName,
            port = port,
            modifier =
                Modifier
                    .size(AlbumCardSize)
                    .clip(AlbumCoverShape)
                    .background(colors.surfaceContainerHigh),
            maxSidePx = 384,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.bucketName,
            style = MiuixTheme.textStyles.body2,
            color = colors.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun ImageGrid(
    images: List<MediaImage>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onReachListEnd: () -> Unit,
    onImageClick: (MediaImage) -> Unit,
    modifier: Modifier = Modifier,
    contentTopPadding: Dp = 0.dp,
    port: PictureSelectorPort,
) {
    val colors = MiuixTheme.colorScheme
    when {
        isRefreshing && images.isEmpty() ->
            PlaceholderText("正在读取图片...", modifier, contentTopPadding)
        images.isEmpty() ->
            PlaceholderText("没有找到图片", modifier, contentTopPadding)
        else -> {
            val gridState = rememberLazyGridState()
            WatchGridTail(
                gridState = gridState,
                itemCount = images.size,
                onReachEnd = onReachListEnd,
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(top = contentTopPadding, bottom = 24.dp),
            ) {
                items(images, key = { it.id }) { image ->
                    PhotoTile(
                        image = image,
                        onClick = { onImageClick(image) },
                        port = port,
                    )
                }

                if (isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "加载更多中...",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTile(
    image: MediaImage,
    onClick: () -> Unit,
    port: PictureSelectorPort,
) {
    val colors = MiuixTheme.colorScheme

    SelectorThumbnail(
        uri = image.contentUri,
        contentDescription = null,
        port = port,
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(PhotoShape)
                .background(colors.surfaceContainerHigh)
                .clickable(onClick = onClick),
        maxSidePx = 360,
    )
}

@Composable
private fun SelectorThumbnail(
    uri: String,
    contentDescription: String?,
    port: PictureSelectorPort,
    modifier: Modifier,
    maxSidePx: Int,
) {
    if (uri.startsWith(IosAssetUriPrefix)) {
        val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
            initialValue = null,
            key1 = uri,
            key2 = maxSidePx,
            key3 = port,
        ) {
            value = withContext(AppIoDispatcher) {
                port.loadThumbnail(uri, maxSidePx)
            }
        }
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        } else {
            Box(modifier = modifier)
        }
    } else {
        AsyncImage(
            model = toAsyncImageModel(uri),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Composable
private fun WatchGridTail(
    gridState: LazyGridState,
    itemCount: Int,
    onReachEnd: () -> Unit,
) {
    LaunchedEffect(gridState, itemCount) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collectLatest { lastVisibleIndex ->
                if (itemCount > 0 && lastVisibleIndex >= itemCount - 6) {
                    onReachEnd()
                }
            }
    }
}

@Composable
private fun PlaceholderText(
    text: String,
    modifier: Modifier = Modifier,
    contentTopPadding: Dp = 0.dp,
) {
    val colors = MiuixTheme.colorScheme
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = contentTopPadding, bottom = 24.dp),
    ) {
        item(key = "placeholder") {
            Box(
                modifier = Modifier.fillParentMaxSize().heightIn(min = 240.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = text, color = colors.onSurfaceVariantSummary)
            }
        }
    }
}
