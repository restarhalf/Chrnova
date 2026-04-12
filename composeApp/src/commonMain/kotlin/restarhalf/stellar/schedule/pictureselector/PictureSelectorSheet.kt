package restarhalf.stellar.schedule.pictureselector

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Close
import restarhalf.stellar.schedule.ui.image.toAsyncImageModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixShape
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import androidx.compose.foundation.lazy.items as lazyItems

private val SheetContentHeight = 620.dp
private val SheetContentMinHeight = 360.dp
private val SheetContentVerticalPadding = 24.dp

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

    LaunchedEffect(show, hasPermission) {
        if (!show) {
            state.resetTransientState()
            return@LaunchedEffect
        }
        if (hasPermission) {
            state.refresh()
        }
    }

    val cropTarget = state.cropTarget
    if (cropTarget != null) {
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

    WindowBottomSheet(
        show = true,
        modifier = Modifier,
        title = "选择图片",
        startAction = {
            IconButton(
                onClick = {
                    state.resetTransientState()
                    currentOnDismiss()
                },
            ) {
                Icon(imageVector = Close, contentDescription = "关闭")
            }
        },
        endAction = null,
        backgroundColor = BottomSheetDefaults.backgroundColor(),
        enableWindowDim = true,
        cornerRadius = BottomSheetDefaults.cornerRadius,
        sheetMaxWidth = BottomSheetDefaults.maxWidth,
        onDismissRequest = {
            state.resetTransientState()
            currentOnDismiss()
        },
        onDismissFinished = null,
        outsideMargin = BottomSheetDefaults.outsideMargin,
        insideMargin = BottomSheetDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        dragHandleColor = MiuixTheme.colorScheme.surface,
        allowDismiss = false,
        enableNestedScroll = true,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxAvailableHeight = (maxHeight - SheetContentVerticalPadding).coerceAtLeast(1.dp)
            val cappedHeight = maxAvailableHeight.coerceAtMost(SheetContentHeight)
            val adaptiveHeight =
                if (maxAvailableHeight >= SheetContentMinHeight) {
                    cappedHeight.coerceAtLeast(SheetContentMinHeight)
                } else {
                    cappedHeight
                }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(adaptiveHeight)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!hasPermission) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        PermissionContent(
                            summary = permissionSummary,
                            onRequestPermission = onRequestPermission,
                        )
                    }
                } else {
                    SelectorTabs(
                        selectedTab = state.selectedTab,
                        onSelectTab = state::selectTab,
                    )

                    if (state.selectedTab == PictureSelectorTab.Albums && state.currentAlbum != null) {
                        AlbumBreadcrumb(
                            album = state.currentAlbum!!,
                            onBack = { state.backToAlbumList() },
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        when {
                            state.selectedTab == PictureSelectorTab.All -> {
                                ImageGrid(
                                    images = state.allImages,
                                    isRefreshing = state.isRefreshing,
                                    isLoadingMore = state.isLoadingMore,
                                    onReachListEnd = { scope.launch { state.loadMoreIfNeeded() } },
                                    onImageClick = state::openCropper,
                                    modifier = Modifier.fillMaxSize(),
                                    port = port,
                                )
                            }

                            state.currentAlbum == null -> {
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

                            else -> {
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
    }
}

@Composable
private fun PermissionContent(
    summary: String,
    onRequestPermission: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = summary, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Button(
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = onRequestPermission,
            ) {
                Text(text = "授予权限", color = MiuixTheme.colorScheme.onPrimary)
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
private fun AlbumBreadcrumb(
    album: MediaAlbum,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(miuixShape(8.dp))
                .background(MiuixTheme.colorScheme.surface)
                .clickable(onClick = onBack)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "返回相册列表",
            color = MiuixTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = album.bucketName,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
            LazyColumn(
                modifier = modifier.fillMaxWidth().heightIn(min = 280.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                lazyItems(albums, key = { it.bucketId }) { album ->
                    AlbumRow(album = album, onClick = { onAlbumClick(album) }, port = port)
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
    AppCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectorThumbnail(
                uri = album.coverUri,
                contentDescription = album.bucketName,
                port = port,
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(miuixShape(6.dp))
                        .background(MiuixTheme.colorScheme.surface),
                maxSidePx = 192,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = album.bucketName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${album.count} 张",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
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
    port: PictureSelectorPort,
) {
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
                modifier = modifier.fillMaxWidth().heightIn(min = 320.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(images, key = { it.id }) { image ->
                    SelectorThumbnail(
                        uri = image.contentUri,
                        contentDescription = null,
                        port = port,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(miuixShape(8.dp))
                                .background(MiuixTheme.colorScheme.surface)
                                .clickable { onImageClick(image) },
                        maxSidePx = 360,
                    )
                }

                if (isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "加载更多中...",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
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
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
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
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 320.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
