package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

interface BackgroundSettingsPort {
    fun observeBackgroundImageUri(): Flow<String?>
    fun setBackgroundImageUri(uri: String?)

    fun observeBackgroundAlpha(): Flow<Float>
    fun setBackgroundAlpha(value: Float)

    fun observeBackgroundBlur(): Flow<Float>
    fun setBackgroundBlur(value: Float)

    fun observeComponentsAlpha(): Flow<Float>
    fun setComponentsAlpha(value: Float)
}
