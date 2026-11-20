package restarhalf.stellar.schedule.domain.port

import androidx.compose.ui.graphics.ImageBitmap

interface WallpaperPort {

    fun peekWallpaper(): ImageBitmap?

    suspend fun loadWallpaper(): ImageBitmap?

    suspend fun importWallpaper(uri: String): ImageBitmap?

    suspend fun clearWallpaper()

    fun wallpaperFilePath(): String?

    fun wallpaperIsLight(image: ImageBitmap?): Boolean?
}
