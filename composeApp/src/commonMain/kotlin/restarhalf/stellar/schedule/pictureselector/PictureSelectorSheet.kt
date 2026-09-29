package restarhalf.stellar.schedule.pictureselector

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Close
import restarhalf.stellar.schedule.ui.image.toAsyncImageModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import androidx.compose.foundation.lazy.items as lazyItems

private val PhotoShape = RoundedCornerShape(10.dp)
private val AlbumCoverShape = RoundedCornerShape(12.dp)

/** 页面状态：Tab + 是否已进入相册二级页。 */
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
    outputWidthPx: Int,
    outputHeightPx: Int,
    onDismissRequest: () -> Unit,
    onPicked: (String) -> Unit,
    port: PictureSelectorPort,
) {
    if (!show) return

    val state = rememberPictureSelectorState(port)
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(show, hasPermission) {
        if (!show) {
            state.resetTransientState()
            return@LaunchedEffect
        }
        if (hasPermission) {
            state.refresh()
        }
    }

    // 逐级退出：裁剪 → 相册二级 → 关闭选择器
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
    if (cropTarget != null) {
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

    val page = SelectorPage(tab = state.selectedTab, album = state.currentAlbum)
    val inAlbumDetail = page.inAlbumDetail
    val sheetBackground = BottomSheetDefaults.backgroundColor()

    WindowBottomSheet(
        show = true,
        modifier = Modifier,
        title = if (inAlbumDetail) page.album?.bucketName ?: "选择图片" else "选择图片",
        startAction = {
            IconButton(onClick = goBackOrDismiss) {
                if (inAlbumDetail) {
                    Icon(imageVector = Back, contentDescription = "返回")
                } else {
                    Icon(imageVector = Close, contentDescription = "关闭")
                }
            }
        },
        endAction = null,
        backgroundColor = sheetBackground,
        enableWindowDim = false,
        cornerRadius = BottomSheetDefaults.cornerRadius,
        sheetMaxWidth = Dp.Infinity,
        onDismissRequest = goBackOrDismiss,
        onDismissFinished = null,
        outsideMargin = DpSize.Zero,
        insideMargin = DpSize.Zero,
        defaultWindowInsetsPadding = true,
        dragHandleColor = colors.surface,
        // 系统返回自己接管，避免 Sheet 整层被 dismiss
        allowDismiss = false,
        enableNestedScroll = true,
    ) {
        // 必须写在 Sheet 内容里，才能挂到 Sheet 所在窗口的返回栈
        val sheetBackState = rememberNavigationEventState(NavigationEventInfo.None)
        NavigationBackHandler(
            state = sheetBackState,
            isBackEnabled = true,
            onBackCompleted = goBackOrDismiss,
        )

        if (!hasPermission) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                PermissionContent(
                    summary = permissionSummary,
                    onRequestPermission = onRequestPermission,
                )
            }
        } else {
            // 二级页覆盖整块 body（含 TabRow）
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                    // 一级：Tab + 全部/相册列表
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SelectorTabs(
                            selectedTab = state.selectedTab,
                            onSelectTab = state::selectTab,
                        )

                        val rootTarget =
                            if (state.selectedTab == PictureSelectorTab.All) {
                                PictureSelectorTab.All
                            } else {
                                PictureSelectorTab.Albums
                            }

                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            AnimatedContent(
                                targetState = rootTarget,
                                transitionSpec = {
                                    val forward = targetState.ordinal >= initialState.ordinal
                                    val enterOffset: (Int) -> Int = { width -> if (forward) width / 5 else -width / 5 }
                                    val exitOffset: (Int) -> Int = { width -> if (forward) -width / 8 else width / 8 }
                                    (slideInHorizontally(
                                        animationSpec = tween(TabDurationMs, easing = EaseOutFast),
                                        initialOffsetX = enterOffset,
                                    ) + fadeIn(tween(TabDurationMs, easing = EaseOutFast)))
                                        .togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = tween(TabDurationMs, easing = EaseInFast),
                                                targetOffsetX = exitOffset,
                                            ) + fadeOut(tween(TabDurationMs / 2, easing = EaseInFast)),
                                        )
                                },
                                label = "selectorRoot",
                            ) { target ->
                                if (target == PictureSelectorTab.All) {
                                    ImageGrid(
                                        images = state.allImages,
                                        isRefreshing = state.isRefreshing,
                                        isLoadingMore = state.isLoadingMore,
                                        onReachListEnd = { scope.launch { state.loadMoreIfNeeded() } },
                                        onImageClick = state::openCropper,
                                        modifier = Modifier.fillMaxSize(),
                                        port = port,
                                    )
                                } else {
                                    AlbumList(
                                        albums = state.albums,
                                        isRefreshing = state.isRefreshing,
                                        onAlbumClick = { album ->
                                            scope.launch { state.openAlbum(album) }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        port = port,
                                    )
                                }
                            }
                        }
                    }

                    // 二级：整页上推，直接盖住 TabRow 与一级内容
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
                                    .background(sheetBackground),
                        ) {
                            ImageGrid(
                                images = state.currentAlbumImages,
                                isRefreshing = state.isRefreshing,
                                isLoadingMore = state.isLoadingMore,
                                onReachListEnd = { scope.launch { state.loadMoreIfNeeded() } },
                                onImageClick = state::openCropper,
                                modifier = Modifier.fillMaxSize(),
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
    selectedTab: PictureSelectorTab,
    onSelectTab: (PictureSelectorTab) -> Unit,
) {
    TabRowWithContour(
        tabs = listOf("全部图片", "相册"),
        selectedTabIndex = if (selectedTab == PictureSelectorTab.All) 0 else 1,
        onTabSelected = { index ->
            onSelectTab(if (index == 0) PictureSelectorTab.All else PictureSelectorTab.Albums)
        },
    )
}

@Composable
private fun AlbumList(
    albums: List<MediaAlbum>,
    isRefreshing: Boolean,
    onAlbumClick: (MediaAlbum) -> Unit,
    modifier: Modifier = Modifier,
    port: PictureSelectorPort,
) {
    when {
        isRefreshing && albums.isEmpty() -> PlaceholderText("正在读取相册...", modifier)
        albums.isEmpty() -> PlaceholderText("没有找到图片", modifier)
        else -> {
            AppCard(modifier = modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    lazyItems(albums, key = { it.bucketId }) { album ->
                        AlbumRow(album = album, onClick = { onAlbumClick(album) }, port = port)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(
    album: MediaAlbum,
    onClick: () -> Unit,
    port: PictureSelectorPort,
) {
    ArrowPreference(
        title = album.bucketName,
        summary = "${album.count} 张",
        onClick = onClick,
        startAction = {
            SelectorThumbnail(
                uri = album.coverUri,
                contentDescription = album.bucketName,
                port = port,
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(AlbumCoverShape),
                maxSidePx = 192,
            )
        },
    )
}

@Composable
private fun ImageGrid(
    images: List<MediaImage>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onReachListEnd: () -> Unit,
    onImageClick: (MediaImage) -> Unit,
    modifier: Modifier = Modifier,
    port: PictureSelectorPort,
) {
    val colors = MiuixTheme.colorScheme
    when {
        isRefreshing && images.isEmpty() -> PlaceholderText("正在读取图片...", modifier)
        images.isEmpty() -> PlaceholderText("没有找到图片", modifier)
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
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
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 280.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = colors.onSurfaceVariantSummary)
    }
}
