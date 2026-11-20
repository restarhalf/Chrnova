package restarhalf.stellar.schedule.data.impl

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.data.wallpaper.WallpaperStore
import restarhalf.stellar.schedule.domain.port.WallpaperPort
import restarhalf.stellar.schedule.platform.AppIoDispatcher

class WallpaperPortImpl : WallpaperPort {

    override fun peekWallpaper(): ImageBitmap? =
        WallpaperStore.currentImageBitmap()
            ?: WallpaperStore.synchronousLoader?.invoke()?.also { WallpaperStore.setCached(it) }

    override suspend fun loadWallpaper(): ImageBitmap? = WallpaperStore.load()

    override suspend fun importWallpaper(uri: String): ImageBitmap? {
        val bitmap = withContext(AppIoDispatcher) {
            WallpaperStore.backend?.decodeUri(uri)
        } ?: return null
        return if (WallpaperStore.save(bitmap)) WallpaperStore.currentImageBitmap() else null
    }

    override suspend fun clearWallpaper() {
        WallpaperStore.clear()
    }

    override fun wallpaperFilePath(): String? = WallpaperStore.filePath()

    override fun wallpaperIsLight(image: ImageBitmap?): Boolean? =
        WallpaperStore.measureLightness(image)
}
