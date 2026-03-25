package restarhalf.stellar.schedule.ui.koin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import org.koin.core.qualifier.Qualifier
import org.koin.mp.KoinPlatform

@Composable
inline fun <reified T : ViewModel> koinViewModel(qualifier: Qualifier? = null): T {
    return remember {
        KoinPlatform.getKoin().get<T>(qualifier)
    }
}
