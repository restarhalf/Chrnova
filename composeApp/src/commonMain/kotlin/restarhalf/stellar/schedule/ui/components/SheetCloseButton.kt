package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.ui.icons.Close

@Composable
fun SheetCloseButton(onDismissRequest: () -> Unit) {
    SheetGlassButton(
        icon = Close,
        contentDescription = "关闭",
        onClick = onDismissRequest,
        modifier = Modifier.padding(start = 18.dp),
    )
}
