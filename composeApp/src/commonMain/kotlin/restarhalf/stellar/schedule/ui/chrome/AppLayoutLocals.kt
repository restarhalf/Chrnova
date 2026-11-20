package restarhalf.stellar.schedule.ui.chrome

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalAppScaffoldPadding = staticCompositionLocalOf { PaddingValues(0.dp) }

val LocalSystemBarsDarkOverride = staticCompositionLocalOf<MutableState<Boolean?>> { mutableStateOf(null) }
