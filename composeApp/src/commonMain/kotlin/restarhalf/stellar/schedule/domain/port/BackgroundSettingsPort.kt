package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

interface BackgroundSettingsPort {
    
    fun getBackgroundImageUri(): String?
    
    fun observeBackgroundImageUri(): Flow<String?>
    
    fun setBackgroundImageUri(uri: String?)

    fun getBackgroundAlpha(): Float
    
    fun observeBackgroundAlpha(): Flow<Float>
    
    fun setBackgroundAlpha(value: Float)

    fun getBackgroundBlur(): Float
    
    fun observeBackgroundBlur(): Flow<Float>
    
    fun setBackgroundBlur(value: Float)

    fun getComponentsAlpha(): Float
    
    fun observeComponentsAlpha(): Flow<Float>
    
    fun setComponentsAlpha(value: Float)
}
