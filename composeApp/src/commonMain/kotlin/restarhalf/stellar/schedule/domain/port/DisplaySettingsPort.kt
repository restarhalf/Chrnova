package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

interface DisplaySettingsPort {
    companion object {

        const val DEFAULT_ROW_HEIGHT_DP = 64

        const val MIN_ROW_HEIGHT_DP = 48

        const val MAX_ROW_HEIGHT_DP = 120
    }

    fun observeShowNonCurrentWeek(): Flow<Boolean>

    fun setShowNonCurrentWeek(show: Boolean)

    fun observeScheduleRowHeight(): Flow<Int>

    fun setScheduleRowHeight(heightDp: Int)

    fun observeThemeMode(): Flow<Int>

    fun getThemeMode(): Int

    fun setThemeMode(mode: Int)

    fun observeFloatingBar(): Flow<Int>

    fun getFloatingBar(): Int

    fun setFloatingBar(mode: Int)
}
