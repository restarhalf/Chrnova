package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getFloatFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.domain.model.CardEdgeThickness
import restarhalf.stellar.schedule.domain.model.CardRefractionLevel
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

@OptIn(ExperimentalSettingsApi::class)
class BackgroundSettingsPortImpl(
    private val settings: ObservableSettings,
) : BackgroundSettingsPort {

    override fun getBackgroundImageUri(): String? =
        settings.getStringOrNull(SettingsKeys.BACKGROUND_IMAGE_URI)

    override fun observeBackgroundImageUri(): Flow<String?> {
        return settings.getStringOrNullFlow(SettingsKeys.BACKGROUND_IMAGE_URI)
    }

    override fun setBackgroundImageUri(uri: String?) {
        settings[SettingsKeys.BACKGROUND_IMAGE_URI] = uri
    }

    override fun getBackgroundAlpha(): Float =
        settings.getFloat(SettingsKeys.BACKGROUND_ALPHA, 1f)

    override fun observeBackgroundAlpha(): Flow<Float> {
        return settings.getFloatFlow(SettingsKeys.BACKGROUND_ALPHA, 1f)
    }

    override fun setBackgroundAlpha(value: Float) {
        settings[SettingsKeys.BACKGROUND_ALPHA] = value
    }

    override fun getBackgroundBlur(): Float =
        settings.getFloat(SettingsKeys.BACKGROUND_BLUR, 0f)

    override fun observeBackgroundBlur(): Flow<Float> {
        return settings.getFloatFlow(SettingsKeys.BACKGROUND_BLUR, 0f)
    }

    override fun setBackgroundBlur(value: Float) {
        settings[SettingsKeys.BACKGROUND_BLUR] = value
    }

    override fun getBackgroundBrightness(): Float =
        settings.getFloat(SettingsKeys.BACKGROUND_BRIGHTNESS, 0f)

    override fun observeBackgroundBrightness(): Flow<Float> {
        return settings.getFloatFlow(SettingsKeys.BACKGROUND_BRIGHTNESS, 0f)
    }

    override fun setBackgroundBrightness(value: Float) {
        settings[SettingsKeys.BACKGROUND_BRIGHTNESS] = value
    }

    override fun getWallpaperOffsetX(): Float =
        settings.getFloat(SettingsKeys.WALLPAPER_OFFSET_X, 0f)

    override fun getWallpaperOffsetY(): Float =
        settings.getFloat(SettingsKeys.WALLPAPER_OFFSET_Y, 0f)

    override fun setWallpaperOffset(x: Float, y: Float) {
        settings[SettingsKeys.WALLPAPER_OFFSET_X] = x
        settings[SettingsKeys.WALLPAPER_OFFSET_Y] = y
    }

    override fun getWallpaperScale(): Float =
        settings.getFloat(SettingsKeys.WALLPAPER_SCALE, 0f)

    override fun setWallpaperScale(value: Float) {
        settings[SettingsKeys.WALLPAPER_SCALE] = value
    }

    override fun getWallpaperIsDark(): Boolean? =
        settings.getBooleanOrNull(SettingsKeys.WALLPAPER_IS_DARK)

    override fun setWallpaperIsDark(value: Boolean?) {
        if (value == null) {
            settings.remove(SettingsKeys.WALLPAPER_IS_DARK)
        } else {
            settings[SettingsKeys.WALLPAPER_IS_DARK] = value
        }
    }

    override fun getComponentsAlpha(): Float =
        settings.getFloat(
            SettingsKeys.COMPONENTS_ALPHA,
            BackgroundSettingsPort.DEFAULT_COMPONENTS_ALPHA,
        )

    override fun observeComponentsAlpha(): Flow<Float> {
        return settings.getFloatFlow(
            SettingsKeys.COMPONENTS_ALPHA,
            BackgroundSettingsPort.DEFAULT_COMPONENTS_ALPHA,
        )
    }

    override fun setComponentsAlpha(value: Float) {
        settings[SettingsKeys.COMPONENTS_ALPHA] = value
    }

    override fun getCardBlurPx(): Float =
        settings.getFloat(
            SettingsKeys.CARD_BLUR_PX,
            BackgroundSettingsPort.DEFAULT_CARD_BLUR_PX,
        ).coerceIn(0f, BackgroundSettingsPort.MAX_CARD_BLUR_PX)

    override fun observeCardBlurPx(): Flow<Float> =
        settings.getFloatFlow(
            SettingsKeys.CARD_BLUR_PX,
            BackgroundSettingsPort.DEFAULT_CARD_BLUR_PX,
        ).map { it.coerceIn(0f, BackgroundSettingsPort.MAX_CARD_BLUR_PX) }

    override fun setCardBlurPx(value: Float) {
        settings[SettingsKeys.CARD_BLUR_PX] = value.coerceIn(0f, BackgroundSettingsPort.MAX_CARD_BLUR_PX)
    }

    override fun getCardRefraction(): CardRefractionLevel =
        CardRefractionLevel.fromOrdinal(
            settings.getInt(SettingsKeys.CARD_REFRACTION_LEVEL, CardRefractionLevel.DEFAULT.ordinal),
        )

    override fun observeCardRefraction(): Flow<CardRefractionLevel> =
        settings.getIntFlow(
            SettingsKeys.CARD_REFRACTION_LEVEL,
            CardRefractionLevel.DEFAULT.ordinal,
        ).map(CardRefractionLevel::fromOrdinal)

    override fun setCardRefraction(value: CardRefractionLevel) {
        settings[SettingsKeys.CARD_REFRACTION_LEVEL] = value.ordinal
    }

    override fun getCardEdgeThickness(): CardEdgeThickness =
        CardEdgeThickness.fromOrdinal(
            settings.getInt(SettingsKeys.CARD_EDGE_THICKNESS, CardEdgeThickness.DEFAULT.ordinal),
        )

    override fun observeCardEdgeThickness(): Flow<CardEdgeThickness> =
        settings.getIntFlow(
            SettingsKeys.CARD_EDGE_THICKNESS,
            CardEdgeThickness.DEFAULT.ordinal,
        ).map(CardEdgeThickness::fromOrdinal)

    override fun setCardEdgeThickness(value: CardEdgeThickness) {
        settings[SettingsKeys.CARD_EDGE_THICKNESS] = value.ordinal
    }

    override fun getCardRim(): Boolean =
        settings.getBoolean(
            SettingsKeys.CARD_RIM_ENABLED,
            BackgroundSettingsPort.DEFAULT_CARD_RIM,
        )

    override fun observeCardRim(): Flow<Boolean> =
        settings.getBooleanFlow(
            SettingsKeys.CARD_RIM_ENABLED,
            BackgroundSettingsPort.DEFAULT_CARD_RIM,
        )

    override fun setCardRim(value: Boolean) {
        settings[SettingsKeys.CARD_RIM_ENABLED] = value
    }
}
