package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.CardEdgeThickness
import restarhalf.stellar.schedule.domain.model.CardRefractionLevel

interface BackgroundSettingsPort {
    companion object {
        const val DEFAULT_COMPONENTS_ALPHA = 0.34f
        const val DEFAULT_CARD_BLUR_PX = 12f
        const val MAX_CARD_BLUR_PX = 24f
        const val DEFAULT_CARD_RIM = true
    }
    
    fun getBackgroundImageUri(): String?
    
    fun observeBackgroundImageUri(): Flow<String?>
    
    fun setBackgroundImageUri(uri: String?)

    fun getBackgroundAlpha(): Float
    
    fun observeBackgroundAlpha(): Flow<Float>
    
    fun setBackgroundAlpha(value: Float)

    fun getBackgroundBlur(): Float

    fun observeBackgroundBlur(): Flow<Float>

    fun setBackgroundBlur(value: Float)

    fun getBackgroundBrightness(): Float

    fun observeBackgroundBrightness(): Flow<Float>

    fun setBackgroundBrightness(value: Float)

    fun getWallpaperOffsetX(): Float

    fun getWallpaperOffsetY(): Float

    fun setWallpaperOffset(x: Float, y: Float)

    fun getWallpaperScale(): Float

    fun setWallpaperScale(value: Float)

    fun getWallpaperIsDark(): Boolean?

    fun setWallpaperIsDark(value: Boolean?)

    fun getComponentsAlpha(): Float
    
    fun observeComponentsAlpha(): Flow<Float>
    
    fun setComponentsAlpha(value: Float)

    fun getCardBlurPx(): Float

    fun observeCardBlurPx(): Flow<Float>

    fun setCardBlurPx(value: Float)

    fun getCardRefraction(): CardRefractionLevel

    fun observeCardRefraction(): Flow<CardRefractionLevel>

    fun setCardRefraction(value: CardRefractionLevel)

    fun getCardEdgeThickness(): CardEdgeThickness

    fun observeCardEdgeThickness(): Flow<CardEdgeThickness>

    fun setCardEdgeThickness(value: CardEdgeThickness)

    fun getCardRim(): Boolean

    fun observeCardRim(): Flow<Boolean>

    fun setCardRim(value: Boolean)
}
