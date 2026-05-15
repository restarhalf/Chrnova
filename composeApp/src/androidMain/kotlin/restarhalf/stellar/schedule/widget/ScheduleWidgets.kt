package restarhalf.stellar.schedule.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent

class TodaySmallWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val snapshot = WidgetDataRepository.load(context)

        provideContent { SmallWidgetContent(snapshot.small) }
    }
}

class TodayLargeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val snapshot = WidgetDataRepository.load(context)

        provideContent { LargeWidgetContent(snapshot.large) }
    }
}
