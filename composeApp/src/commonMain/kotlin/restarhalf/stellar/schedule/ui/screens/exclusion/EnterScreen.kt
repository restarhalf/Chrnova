package restarhalf.stellar.schedule.ui.screens.exclusion
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.ui.icons.AppIcon
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EnterScreen(
    onDismissRequest: () -> Unit,
    pagerState: PagerState
) {
    val appInfo: AppInfoPort = koinInject()
    val settings: ObservableSettings = koinInject(named(SettingsKeys.PREFS_NAME))
    val appName = remember(appInfo.appName) { appInfo.appName }
    val go = remember { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme
    val easing = CubicBezierEasing(.42f, 0f, 0.26f, .85f)
    
    val animatedY = animateDpAsState(
        targetValue = if (go.value) (-30).dp else 0.dp,
        animationSpec = tween(durationMillis = 1150, easing = easing),
        label = "enterPagerY"
    )
    val animatedAlpha = animateFloatAsState(
        targetValue = if (go.value) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1150, easing = easing),
        label = "enterPagerAlpha"
    )
    
    LaunchedEffect(pagerState.currentPage) {
        go.value = pagerState.currentPage >= 3
    }
    
    Column {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 100.dp)
                .offset(x = 0.dp, y = animatedY.value)
                .alpha(animatedAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = AppIcon,
                contentDescription = "App Icon",
                tint = colors.onBackground,
                modifier = Modifier.size(90.dp)
            )
            Text(
                text = appName,
                color = colors.onBackground,
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight(560)
            )
            Text(
                text = "设置完成",
                modifier = Modifier.padding(top = 20.dp)
            )
        }
        
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 28.dp),
            colors = ButtonDefaults.buttonColorsPrimary(),
            onClick = {
                settings[SettingsKeys.CONFIRM_PRIVACY] = true
                onDismissRequest()
            }
        ) {
            Text(
                text = "进入软件",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MiuixTheme.textStyles.title4,
                color = colors.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
