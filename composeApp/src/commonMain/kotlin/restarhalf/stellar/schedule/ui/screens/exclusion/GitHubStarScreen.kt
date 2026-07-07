package restarhalf.stellar.schedule.ui.screens.exclusion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.icons.AppIcon
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GitHubStarScreen(pagerState: PagerState, onStarClick: () -> Unit, onNext: () -> Unit) {
    val go = remember { mutableStateOf(false) }
    val easing = CubicBezierEasing(.42f, 0f, 0.26f, .85f)
    val colors = MiuixTheme.colorScheme

    val animatedY = animateDpAsState(
        targetValue = if (go.value) (-30).dp else 0.dp,
        animationSpec = tween(durationMillis = 1150, easing = easing),
        label = "gitHubStarY"
    )
    val animatedAlpha = animateFloatAsState(
        targetValue = if (go.value) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1150, easing = easing),
        label = "gitHubStarAlpha"
    )

    LaunchedEffect(pagerState.currentPage) {
        go.value = pagerState.currentPage >= 4
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .offset(x = 0.dp, y = animatedY.value)
                .alpha(animatedAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = AppIcon,
                contentDescription = "App Icon",
                tint = colors.onBackground,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "喜欢这个应用吗？",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "如果 Chrnova 帮助到了你\n请在 GitHub 上给我一个 Star",
                fontSize = 16.sp,
                color = colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Star 是对我最大的鼓励",
                fontSize = 14.sp,
                color = colors.onBackground.copy(alpha = 0.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                text = "去 Github 点星",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = onStarClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = "下一步",
                modifier = Modifier.fillMaxWidth(),
                onClick = onNext
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
