package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * 背景设置端口接口
 * 
 * 定义背景图片相关设置的抽象接口，包括图片URI、透明度、模糊度等。
 */
interface BackgroundSettingsPort {
    /** 观察背景图片URI变化 */
    fun observeBackgroundImageUri(): Flow<String?>
    /** 设置背景图片URI */
    fun setBackgroundImageUri(uri: String?)

    /** 观察背景透明度变化（0.0-1.0） */
    fun observeBackgroundAlpha(): Flow<Float>
    /** 设置背景透明度（0.0-1.0） */
    fun setBackgroundAlpha(value: Float)

    /** 观察背景模糊度变化（0.0-1.0） */
    fun observeBackgroundBlur(): Flow<Float>
    /** 设置背景模糊度（0.0-1.0） */
    fun setBackgroundBlur(value: Float)

    /** 观察组件透明度变化（0.0-1.0），控制背景上组件的可见度 */
    fun observeComponentsAlpha(): Flow<Float>
    /** 设置组件透明度（0.0-1.0） */
    fun setComponentsAlpha(value: Float)
}
