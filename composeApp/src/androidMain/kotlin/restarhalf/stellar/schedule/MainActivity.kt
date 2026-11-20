package restarhalf.stellar.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import org.koin.android.ext.android.inject
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.widget.WidgetClickHelper
import restarhalf.stellar.schedule.widget.WidgetOnStartRefresher
import restarhalf.stellar.schedule.widget.WidgetRefreshController

class MainActivity : ComponentActivity() {

    private val displaySettings: DisplaySettingsPort by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 小部件点击带过来的标记：消费后强制刷一遍全部小组件（绕过 onStart 的 5s 节流）
        if (WidgetClickHelper.consumeLaunchFlag(intent)) {
            WidgetOnStartRefresher.refreshIfNeeded(applicationContext, lifecycleScope, force = true)
        }
        AppRoot(displaySettings)
    }

    override fun onStart() {
        super.onStart()
        WidgetOnStartRefresher.refreshIfNeeded(applicationContext, lifecycleScope)
    }

    override fun onStop() {
        super.onStop()
        WidgetRefreshController.refreshAsync(applicationContext)
    }
}
