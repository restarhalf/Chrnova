package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getFloatFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

@OptIn(ExperimentalSettingsApi::class)
class BackgroundSettingsPortImpl(
    private val settings: ObservableSettings,
) : BackgroundSettingsPort {

    override fun observeBackgroundImageUri(): Flow<String?> {
        return settings.getStringOrNullFlow(SettingsKeys.BACKGROUND_IMAGE_URI)
    }

    override fun setBackgroundImageUri(uri: String?) {
        settings[SettingsKeys.BACKGROUND_IMAGE_URI] = uri
    }


    override fun observeBackgroundAlpha(): Flow<Float> {
        return settings.getFloatFlow(SettingsKeys.BACKGROUND_ALPHA, 1f)
    }

    override fun setBackgroundAlpha(value: Float) {
        settings[SettingsKeys.BACKGROUND_ALPHA] = value
    }

    override fun observeBackgroundBlur(): Flow<Float> {
        return settings.getFloatFlow(SettingsKeys.BACKGROUND_BLUR, 0f)
    }

    override fun setBackgroundBlur(value: Float) {
        settings[SettingsKeys.BACKGROUND_BLUR] = value
    }

    override fun observeComponentsAlpha(): Flow<Float> {
        return settings.getFloatFlow(SettingsKeys.COMPONENTS_ALPHA, 1f)
    }

    override fun setComponentsAlpha(value: Float) {
        settings[SettingsKeys.COMPONENTS_ALPHA] = value
    }
}
