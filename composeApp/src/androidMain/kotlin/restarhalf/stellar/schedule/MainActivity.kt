package restarhalf.stellar.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.russhwolf.settings.ObservableSettings
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.widget.WidgetOnStartRefresher

class MainActivity : ComponentActivity() {

    private val settings: ObservableSettings by inject(named(SettingsKeys.PREFS_NAME))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRoot(settings)
    }

    override fun onStart() {
        super.onStart()
        WidgetOnStartRefresher.refreshIfNeeded(applicationContext, lifecycleScope)
    }
}
