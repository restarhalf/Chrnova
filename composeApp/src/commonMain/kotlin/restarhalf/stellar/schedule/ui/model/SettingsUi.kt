package restarhalf.stellar.schedule.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
data class SettingsScreenUi(
    val campusOptions: ImmutableList<String>,
    val campusSelectedIndex: Int,
    val themeOptions: ImmutableList<String>,
    val floatingBarOptions: ImmutableList<String>,
    val termStartSummary: String,
    val syncSummary: String,
)

@Stable
data class TermSelectionUi(
    val items: ImmutableList<String>,
    val selectedIndex: Int,
)

@Immutable
data class AccountUi(
    val loggedIn: Boolean,
    val title: String,
    val summary: String,
)
