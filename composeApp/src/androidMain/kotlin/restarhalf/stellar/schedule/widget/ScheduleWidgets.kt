package restarhalf.stellar.schedule.widget

import android.content.Context
import android.content.res.Configuration
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.port.ScheduleSnapshotPort

private object WidgetDeps : KoinComponent {
    val snapshotPort: ScheduleSnapshotPort by inject()
    val displaySettings: DisplaySettingsPort by inject()
}

/**
 * 跟随应用内的主题设置（而不是只看系统的 night 资源），
 * 这样"强制浅色/深色"时小组件配色和课表页一致。
 */
private fun Context.isDarkTheme(): Boolean =
    when (WidgetDeps.displaySettings.getThemeMode()) {
        1 -> false
        2 -> true
        else ->
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
    }

class TodaySmallWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataRepository.load(WidgetDeps.snapshotPort, context.isDarkTheme())
        provideContent { SmallWidgetContent(snapshot.small) }
    }
}

class TodayLargeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataRepository.load(WidgetDeps.snapshotPort, context.isDarkTheme())
        provideContent { LargeWidgetContent(snapshot.large) }
    }
}
