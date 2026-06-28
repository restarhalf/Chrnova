package restarhalf.stellar.schedule.pictureselector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.platform.AppIoDispatcher

private const val DefaultPageSize = 120
private const val DefaultAlbumScanLimit = 4000

enum class PictureSelectorTab {
    All,
    Albums,
}

@Stable
class PictureSelectorState(
    private val port: PictureSelectorPort,
    private val pageSize: Int = DefaultPageSize,
) {
    var selectedTab by mutableStateOf(PictureSelectorTab.All)
        private set

    var allImages by mutableStateOf<List<MediaImage>>(emptyList())
        private set

    var albums by mutableStateOf<List<MediaAlbum>>(emptyList())
        private set

    var currentAlbum by mutableStateOf<MediaAlbum?>(null)
        private set

    var currentAlbumImages by mutableStateOf<List<MediaImage>>(emptyList())
        private set

    var cropTarget by mutableStateOf<MediaImage?>(null)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var isLoadingMore by mutableStateOf(false)
        private set

    var allHasMore by mutableStateOf(true)
        private set

    var albumHasMore by mutableStateOf(true)
        private set

    suspend fun refresh() {
        if (isRefreshing) return
        isRefreshing = true
        try {
            val recentImages =
                withContext(AppIoDispatcher) {
                    port.loadRecentImages(limit = pageSize, offset = 0)
                }
            val loadedAlbums =
                withContext(AppIoDispatcher) {
                    port.loadAlbums(maxScan = DefaultAlbumScanLimit)
                }

            allImages = recentImages
            albums = loadedAlbums
            allHasMore = recentImages.size >= pageSize

            currentAlbum?.let { album ->
                val firstPage =
                    withContext(AppIoDispatcher) {
                        port.loadAlbumImages(
                            bucketId = album.bucketId,
                            limit = pageSize,
                            offset = 0,
                        )
                    }
                currentAlbumImages = firstPage
                albumHasMore = firstPage.size >= pageSize
            }
        } finally {
            isRefreshing = false
        }
    }

    fun selectTab(tab: PictureSelectorTab) {
        selectedTab = tab
        if (tab == PictureSelectorTab.All) {
            currentAlbum = null
        }
    }

    suspend fun openAlbum(album: MediaAlbum) {
        if (currentAlbum?.bucketId == album.bucketId && currentAlbumImages.isNotEmpty()) {
            selectedTab = PictureSelectorTab.Albums
            return
        }

        selectedTab = PictureSelectorTab.Albums
        currentAlbum = album
        isRefreshing = true
        try {
            val firstPage =
                withContext(AppIoDispatcher) {
                    port.loadAlbumImages(
                        bucketId = album.bucketId,
                        limit = pageSize,
                        offset = 0,
                    )
                }
            currentAlbumImages = firstPage
            albumHasMore = firstPage.size >= pageSize
        } finally {
            isRefreshing = false
        }
    }

    fun backToAlbumList() {
        currentAlbum = null
    }

    suspend fun loadMoreIfNeeded() {
        if (isRefreshing || isLoadingMore) return
        when {
            selectedTab == PictureSelectorTab.All && allHasMore -> loadMoreAll()
            selectedTab == PictureSelectorTab.Albums && currentAlbum != null && albumHasMore ->
                currentAlbum?.let { loadMoreAlbum(it) }
        }
    }

    private suspend fun loadMoreAll() {
        isLoadingMore = true
        try {
            val nextPage =
                withContext(AppIoDispatcher) {
                    port.loadRecentImages(limit = pageSize, offset = allImages.size)
                }
            if (nextPage.isNotEmpty()) {
                allImages = allImages + nextPage
            }
            allHasMore = nextPage.size >= pageSize
        } finally {
            isLoadingMore = false
        }
    }

    private suspend fun loadMoreAlbum(album: MediaAlbum) {
        isLoadingMore = true
        try {
            val nextPage =
                withContext(AppIoDispatcher) {
                    port.loadAlbumImages(
                        bucketId = album.bucketId,
                        limit = pageSize,
                        offset = currentAlbumImages.size,
                    )
                }
            if (nextPage.isNotEmpty()) {
                currentAlbumImages = currentAlbumImages + nextPage
            }
            albumHasMore = nextPage.size >= pageSize
        } finally {
            isLoadingMore = false
        }
    }

    fun openCropper(image: MediaImage) {
        cropTarget = image
    }

    fun closeCropper() {
        cropTarget = null
    }

    fun resetTransientState() {
        selectedTab = PictureSelectorTab.All
        currentAlbum = null
        currentAlbumImages = emptyList()
        cropTarget = null
    }
}

@Composable
fun rememberPictureSelectorState(
    port: PictureSelectorPort,
    pageSize: Int = DefaultPageSize,
): PictureSelectorState = remember(port, pageSize) { PictureSelectorState(port, pageSize) }
