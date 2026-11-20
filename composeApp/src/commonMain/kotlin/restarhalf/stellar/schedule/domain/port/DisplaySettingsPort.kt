package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

interface DisplaySettingsPort {
    companion object {

        const val DEFAULT_ROW_HEIGHT_DP = 64

        const val MIN_ROW_HEIGHT_DP = 48

        const val MAX_ROW_HEIGHT_DP = 120

        const val THEME_MODE_SYSTEM = 0

        const val THEME_MODE_LIGHT = 1

        const val THEME_MODE_DARK = 2

        const val THEME_MODE_FOLLOW_WALLPAPER = 3
    }

    fun observeShowNonCurrentWeek(): Flow<Boolean>

    fun setShowNonCurrentWeek(show: Boolean)

    fun observeScheduleRowHeight(): Flow<Int>

    fun setScheduleRowHeight(heightDp: Int)

    fun observeThemeMode(): Flow<Int>

    fun getThemeMode(): Int

    fun setThemeMode(mode: Int)

    /** 桌面小组件的上下留白档位：0=标准 1=4×6 2=4×7 3=4×6 无字 4=4×7 无字 */
    fun getWidgetPaddingMode(): Int

    fun setWidgetPaddingMode(mode: Int)
}
