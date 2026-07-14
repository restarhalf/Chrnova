package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getFloatFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

/**
 * 背景设置端口实现类
 * 
 * 实现BackgroundSettingsPort接口，负责背景图片相关设置的读写操作。
 * 包括背景图片URI、透明度、模糊度、组件透明度等。
 * 
 * @param settings ObservableSettings实例
 */
@OptIn(ExperimentalSettingsApi::class)
class BackgroundSettingsPortImpl(
    private val settings: ObservableSettings,
) : BackgroundSettingsPort {

    override fun getBackgroundImageUri(): String? =
        settings.getStringOrNull(SettingsKeys.BACKGROUND_IMAGE_URI)

    /**
     * 观察背景图片URI变化
     *
     * @return 背景图片URI Flow
     */
    override fun observeBackgroundImageUri(): Flow<String?> {
        return settings.getStringOrNullFlow(SettingsKeys.BACKGROUND_IMAGE_URI)
    }

    /**
     * 设置背景图片URI
     * 
     * @param uri 图片URI，null表示移除背景
     */
    override fun setBackgroundImageUri(uri: String?) {
        settings[SettingsKeys.BACKGROUND_IMAGE_URI] = uri
    }

    override fun getBackgroundAlpha(): Float =
        settings.getFloat(SettingsKeys.BACKGROUND_ALPHA, 1f)

    /**
     * 观察背景透明度变化
     *
     * @return 背景透明度Flow（0.0-1.0）
     */
    override fun observeBackgroundAlpha(): Flow<Float> {
        return settings.getFloatFlow(SettingsKeys.BACKGROUND_ALPHA, 1f)
    }

    /**
     * 设置背景透明度
     * 
     * @param value 透明度（0.0-1.0）
     */
    override fun setBackgroundAlpha(value: Float) {
        settings[SettingsKeys.BACKGROUND_ALPHA] = value
    }

    override fun getBackgroundBlur(): Float =
        settings.getFloat(SettingsKeys.BACKGROUND_BLUR, 0f)

    /**
     * 观察背景模糊度变化
     *
     * @return 背景模糊度Flow（0.0-1.0）
     */
    override fun observeBackgroundBlur(): Flow<Float> {
        return settings.getFloatFlow(SettingsKeys.BACKGROUND_BLUR, 0f)
    }

    /**
     * 设置背景模糊度
     * 
     * @param value 模糊度（0.0-1.0）
     */
    override fun setBackgroundBlur(value: Float) {
        settings[SettingsKeys.BACKGROUND_BLUR] = value
    }

    override fun getComponentsAlpha(): Float =
        settings.getFloat(SettingsKeys.COMPONENTS_ALPHA, 1f)

    /**
     * 观察组件透明度变化
     *
     * @return 组件透明度Flow（0.0-1.0）
     */
    override fun observeComponentsAlpha(): Flow<Float> {
        return settings.getFloatFlow(SettingsKeys.COMPONENTS_ALPHA, 1f)
    }

    /**
     * 设置组件透明度
     * 
     * @param value 透明度（0.0-1.0）
     */
    override fun setComponentsAlpha(value: Float) {
        settings[SettingsKeys.COMPONENTS_ALPHA] = value
    }
}
