package restarhalf.stellar.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import org.koin.android.ext.android.inject
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.widget.WidgetOnStartRefresher

class MainActivity : ComponentActivity() {

    private val displaySettings: DisplaySettingsPort by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRoot(displaySettings)
    }

    override fun onStart() {
        super.onStart()
        WidgetOnStartRefresher.refreshIfNeeded(applicationContext, lifecycleScope)
    }
}
