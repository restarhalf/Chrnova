package restarhalf.stellar.schedule.ui.screens.foodroulette

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoodRouletteScreen(
    onBack: () -> Unit,
    onFoodSelected: (FoodItem) -> Unit,
    foods: List<FoodItem> = defaultFoodItems,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val colors = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val paddingItemCount = 4
    val itemHeight = 64.dp
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                0
            } else {
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closest =
                    visibleItems.minBy { info ->
                        kotlin.math.abs((info.offset + info.size / 2) - viewportCenter)
                    }
                closest.index.coerceIn(0, (foods.size - 1).coerceAtLeast(0))
            }
        }
    }

    LaunchedEffect(listState, foods.size) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "今天吃什么",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回",
                            tint = colors.onBackground,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                LazyColumn(
                    state = listState,
                    flingBehavior = flingBehavior,
                    contentPadding = PaddingValues(vertical = itemHeight * paddingItemCount),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(foods, key = { index, food -> food.name + index }) { index, food ->
                        val isSelected = index == centerIndex
                        FoodWheelItem(
                            food = food,
                            isSelected = isSelected,
                            itemHeight = itemHeight,
                            onClick = {
                                onFoodSelected(food)
                            },
                        )
                    }
                }

                // 中间选中高亮指示器
                Box(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth(0.85f)
                        .height(itemHeight)
                        .align(Alignment.Center)
                        .squircleBackground(colors.primary.copy(alpha = 0.08f), 16.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(0.7f)
                    .height(52.dp),
                onClick = {
                    if (foods.isNotEmpty()) {
                        val randomIndex = foods.indices.random()
                        scope.launch {
                            listState.animateScrollToItem(
                                index = randomIndex,
                                scrollOffset = -(with(density) { itemHeight.roundToPx() } * paddingItemCount)
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = "随机",
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    color = colors.onPrimary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(0.7f)
                    .height(52.dp),
                onClick = {
                    if (foods.isNotEmpty()) {
                        onFoodSelected(foods[centerIndex])
                    }
                },
            ) {
                Text(
                    text = "选这个，看二维码",
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FoodWheelItem(
    food: FoodItem,
    isSelected: Boolean,
    itemHeight: Dp,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .alpha(if (isSelected) 1f else 0.35f)
            .padding(horizontal = 24.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = food.name,
            textAlign = TextAlign.Center,
            color = if (isSelected) colors.primary else colors.onSurface,
            fontSize = if (isSelected) 26.sp else 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
