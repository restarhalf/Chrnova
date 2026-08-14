package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PersonalInfoCard(
    avatarUri: String?,
    nickname: String,
    academyName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            avatarUri = avatarUri,
            contentDescription = "用户头像",
            size = 64.dp,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = nickname,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                color = colors.onBackground,
            )
            Text(
                text = academyName,
                style = MiuixTheme.textStyles.body2,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
}
