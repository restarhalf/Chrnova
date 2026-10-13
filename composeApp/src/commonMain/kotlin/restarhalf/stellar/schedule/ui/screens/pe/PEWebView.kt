package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
expect fun PEWebView(
    targetUrl: String,
    authScript: String,
    modifier: Modifier = Modifier,
)
