package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.domain.model.CardEdgeThickness
import restarhalf.stellar.schedule.domain.model.CardRefractionLevel
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import restarhalf.stellar.schedule.domain.port.WallpaperPort

class BackgroundViewModel(
    private val backgroundSettings: BackgroundSettingsPort,
    private val wallpaper: WallpaperPort,
) : ViewModel() {

    @Immutable
    data class BackgroundUiState(
        val wallpaper: ImageBitmap?,
        val wallpaperPath: String?,
        val wallpaperOffset: Offset,
        val wallpaperScale: Float,
        val wallpaperIsDark: Boolean?,
        val backgroundAlpha: Float,
        val backgroundBlur: Float,
        val backgroundBrightness: Float,
        val componentsAlpha: Float,
        val cardBlurPx: Float = BackgroundSettingsPort.DEFAULT_CARD_BLUR_PX,
        val cardRefraction: CardRefractionLevel = CardRefractionLevel.DEFAULT,
        val cardEdgeThickness: CardEdgeThickness = CardEdgeThickness.DEFAULT,
        val cardRim: Boolean = BackgroundSettingsPort.DEFAULT_CARD_RIM,
    )

    private data class WallpaperImage(
        val bitmap: ImageBitmap?,
        val path: String?,
        val offset: Offset,
        val scale: Float,
        val isDark: Boolean?,
    )

    private data class CardGlassConfig(
        val cardBlurPx: Float,
        val cardRefraction: CardRefractionLevel,
        val cardEdgeThickness: CardEdgeThickness,
        val cardRim: Boolean,
    )

    private val wallpaperImage = MutableStateFlow(currentWallpaperImage())

    private val baseBackgroundState =
        combine(
            wallpaperImage,
            backgroundSettings.observeBackgroundAlpha(),
            backgroundSettings.observeBackgroundBlur(),
            backgroundSettings.observeBackgroundBrightness(),
            backgroundSettings.observeComponentsAlpha(),
        ) { image, backgroundAlpha, backgroundBlur, backgroundBrightness, componentsAlpha ->
            BackgroundUiState(
                wallpaper = image.bitmap,
                wallpaperPath = image.path,
                wallpaperOffset = image.offset,
                wallpaperScale = image.scale,
                wallpaperIsDark = image.isDark,
                backgroundAlpha = backgroundAlpha,
                backgroundBlur = backgroundBlur,
                backgroundBrightness = backgroundBrightness,
                componentsAlpha = componentsAlpha,
            )
        }

    private val cardGlassConfigState =
        combine(
            backgroundSettings.observeCardBlurPx(),
            backgroundSettings.observeCardRefraction(),
            backgroundSettings.observeCardEdgeThickness(),
            backgroundSettings.observeCardRim(),
        ) { cardBlurPx, cardRefraction, cardEdgeThickness, cardRim ->
            CardGlassConfig(
                cardBlurPx = cardBlurPx,
                cardRefraction = cardRefraction,
                cardEdgeThickness = cardEdgeThickness,
                cardRim = cardRim,
            )
        }

    val uiState: StateFlow<BackgroundUiState> =
        combine(baseBackgroundState, cardGlassConfigState) { base, glass ->
            base.copy(
                cardBlurPx = glass.cardBlurPx,
                cardRefraction = glass.cardRefraction,
                cardEdgeThickness = glass.cardEdgeThickness,
                cardRim = glass.cardRim,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = initialState(),
            )

    init {
        viewModelScope.launch {
            val loaded = wallpaper.loadWallpaper()
            if (loaded != null) {
                wallpaperImage.value = WallpaperImage(
                    bitmap = loaded,
                    path = wallpaper.wallpaperFilePath(),
                    offset = currentOffset(),
                    scale = backgroundSettings.getWallpaperScale(),
                    isDark = backgroundSettings.getWallpaperIsDark(),
                )
                return@launch
            }
            val legacyUri = backgroundSettings.getBackgroundImageUri()
            if (!legacyUri.isNullOrBlank()) {
                val imported = wallpaper.importWallpaper(legacyUri)
                if (imported != null) {
                    wallpaperImage.value = importedWallpaper(imported)
                    backgroundSettings.setBackgroundImageUri(null)
                }
            }
        }
    }

    fun onWallpaperPicked(uri: String) {
        viewModelScope.launch {
            val imported = wallpaper.importWallpaper(uri)
            if (imported != null) {
                wallpaperImage.value = importedWallpaper(imported)
                backgroundSettings.setBackgroundImageUri(null)
            }
        }
    }

    fun onWallpaperCleared() {
        viewModelScope.launch {
            wallpaper.clearWallpaper()
            backgroundSettings.setWallpaperOffset(0f, 0f)
            backgroundSettings.setWallpaperScale(0f)
            backgroundSettings.setWallpaperIsDark(null)
            wallpaperImage.value = WallpaperImage(null, null, Offset.Zero, 0f, null)
            backgroundSettings.setBackgroundImageUri(null)
        }
    }

    fun onWallpaperTransformChanged(offset: Offset, scale: Float) {
        backgroundSettings.setWallpaperOffset(offset.x, offset.y)
        backgroundSettings.setWallpaperScale(scale)
        wallpaperImage.value = wallpaperImage.value.copy(offset = offset, scale = scale)
    }

    fun onBackgroundAlphaChanged(value: Float) {
        backgroundSettings.setBackgroundAlpha(value)
    }

    fun onBackgroundBlurChanged(value: Float) {
        backgroundSettings.setBackgroundBlur(value)
    }

    fun onBackgroundBrightnessChanged(value: Float) {
        backgroundSettings.setBackgroundBrightness(value)
    }

    fun onComponentsAlphaChanged(value: Float) {
        backgroundSettings.setComponentsAlpha(value)
    }

    fun onCardBlurPxChanged(value: Float) {
        backgroundSettings.setCardBlurPx(value)
    }

    fun onCardRefractionChanged(value: CardRefractionLevel) {
        backgroundSettings.setCardRefraction(value)
    }

    fun onCardEdgeThicknessChanged(value: CardEdgeThickness) {
        backgroundSettings.setCardEdgeThickness(value)
    }

    fun onCardRimChanged(value: Boolean) {
        backgroundSettings.setCardRim(value)
    }

    private suspend fun importedWallpaper(bitmap: ImageBitmap): WallpaperImage {
        val isDark = wallpaper.wallpaperIsLight(bitmap)?.let { !it }
        backgroundSettings.setWallpaperOffset(0f, 0f)
        backgroundSettings.setWallpaperScale(0f)
        backgroundSettings.setWallpaperIsDark(isDark)
        return WallpaperImage(
            bitmap = bitmap,
            path = wallpaper.wallpaperFilePath(),
            offset = Offset.Zero,
            scale = 0f,
            isDark = isDark,
        )
    }

    private fun currentOffset(): Offset =
        Offset(backgroundSettings.getWallpaperOffsetX(), backgroundSettings.getWallpaperOffsetY())

    private fun currentWallpaperImage(): WallpaperImage {
        val bitmap = wallpaper.peekWallpaper() ?: return WallpaperImage(
            bitmap = null,
            path = null,
            offset = Offset.Zero,
            scale = 0f,
            isDark = null,
        )
        return WallpaperImage(
            bitmap = bitmap,
            path = wallpaper.wallpaperFilePath(),
            offset = currentOffset(),
            scale = backgroundSettings.getWallpaperScale(),
            isDark = backgroundSettings.getWallpaperIsDark(),
        )
    }

    private fun initialState(): BackgroundUiState {
        val image = wallpaperImage.value
        return BackgroundUiState(
            wallpaper = image.bitmap,
            wallpaperPath = image.path,
            wallpaperOffset = image.offset,
            wallpaperScale = image.scale,
            wallpaperIsDark = image.isDark,
            backgroundAlpha = backgroundSettings.getBackgroundAlpha(),
            backgroundBlur = backgroundSettings.getBackgroundBlur(),
            backgroundBrightness = backgroundSettings.getBackgroundBrightness(),
            componentsAlpha = backgroundSettings.getComponentsAlpha(),
            cardBlurPx = backgroundSettings.getCardBlurPx(),
            cardRefraction = backgroundSettings.getCardRefraction(),
            cardEdgeThickness = backgroundSettings.getCardEdgeThickness(),
            cardRim = backgroundSettings.getCardRim(),
        )
    }
}
