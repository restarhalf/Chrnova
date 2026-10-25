package restarhalf.stellar.schedule.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.domain.port.ScheduleSnapshotPort

private object WidgetDeps : KoinComponent {
    val snapshotPort: ScheduleSnapshotPort by inject()
}

class TodaySmallWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataRepository.load(WidgetDeps.snapshotPort)
        provideContent { SmallWidgetContent(snapshot.small) }
    }
}

class TodayLargeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataRepository.load(WidgetDeps.snapshotPort)
        provideContent { LargeWidgetContent(snapshot.large) }
    }
}
