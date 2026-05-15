package restarhalf.stellar.schedule.ui.screens.exclusion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.ui.icons.Back
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun WelcomeScreen(
    show: MutableState<Boolean>,
    pagerState: PagerState,
    exitApp : () -> Unit,
) {
    val settings: ObservableSettings = koinInject(named(SettingsKeys.PREFS_NAME))
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = pagerState.targetPage != 0,
                enter = fadeIn() + slideIn(
                    animationSpec = tween(
                        durationMillis = 150,
                        delayMillis = 0,
                        easing = LinearEasing
                    )
                ) {
                    IntOffset(it.width, 0)
                },
                exit = fadeOut() + slideOut(
                    animationSpec = tween(
                        durationMillis = 150,
                        delayMillis = 0,
                        easing = LinearEasing
                    )
                ) {
                    IntOffset(it.width, 0)
                }
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val last = pagerState.settledPage - 1
                            pagerState.animateScrollToPage(last)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Back,
                        contentDescription = "back",
                        tint = colorScheme.onBackground
                    )
                }
            }
        }
        
        HorizontalPager(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            state = pagerState,
            userScrollEnabled = false,
            pageContent = { page ->
                when (page) {
                    0 -> WelcomeEnterScreen(pagerState = pagerState)
                    1 -> PrivacyScreen(pagerState = pagerState, onExit = {
                        settings[SettingsKeys.CONFIRM_PRIVACY] = false
                        exitApp()
                    })
                    2 -> EnterScreen(show = show, pagerState = pagerState)
                }
            }
        )
    }
}
