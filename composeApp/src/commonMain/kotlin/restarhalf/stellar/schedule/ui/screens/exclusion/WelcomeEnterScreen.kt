package restarhalf.stellar.schedule.ui.screens.exclusion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import restarhalf.stellar.schedule.ui.icons.Forward
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WelcomeEnterScreen(pagerState: PagerState) {
    val coroutineScope = rememberCoroutineScope()
    val go = remember { mutableStateOf(false) }
    val easing = CubicBezierEasing(.42f, 0f, 0.26f, .85f)
    
    val animatedY = animateDpAsState(
        targetValue = if (go.value) (-30).dp else 0.dp,
        animationSpec = tween(durationMillis = 1150, easing = easing),
        label = "welcomeEnterY"
    )
    val animatedAlpha = animateFloatAsState(
        targetValue = if (go.value) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1150, easing = easing),
        label = "welcomeEnterAlpha"
    )
    val appInfo: AppInfoPort = koinInject()
    val appName = remember(appInfo.appName) { appInfo.appName }
    val colors = MiuixTheme.colorScheme
    LaunchedEffect(pagerState.currentPage) {
        go.value = pagerState.currentPage == 0
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 20.dp)
                .offset(x = 0.dp, y = animatedY.value)
                .alpha(animatedAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "欢迎使用",
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Medium,
                color = colors.onBackground.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = appName,
                color = colors.onBackground,
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight(560)
            )
        }

        Box(
            Modifier
                .size(70.dp)
                .squircleSurface(Color.Transparent, 70.dp)
                .clickable {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Forward,
                contentDescription = "",
                tint = colors.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}

