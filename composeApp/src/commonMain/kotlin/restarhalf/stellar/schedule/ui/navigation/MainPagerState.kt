package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 底部导航栏标签页列表
 * 
 * 定义底部导航栏的页面顺序。
 */
val RootTabs: List<Screen> =
    listOf(
        Screen.Home,        // 首页
        Screen.Schedule,    // 课程表
        Screen.EMS,         // 考试与成绩
        Screen.PEScore,     // 体育成绩
        Screen.Settings,    // 设置
    )

/**
 * 获取Screen在底部导航栏中的索引
 * 
 * @return 索引值，不在列表中返回0
 */
fun Screen.rootTabIndex(): Int = RootTabs.indexOf(this).takeIf { it >= 0 } ?: 0

/**
 * 根据索引获取对应的Screen
 * 
 * @param index 索引
 * @return 对应的Screen，索引越界返回Home
 */
fun rootTabAt(index: Int): Screen = RootTabs.getOrNull(index) ?: Screen.Home

/**
 * 主页面Pager状态的CompositionLocal
 */
val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("No MainPagerState provided!")
}

/**
 * 主页面Pager状态
 * 
 * 管理底部导航栏的页面切换，支持平滑动画。
 * 
 * @param pagerState PagerState实例
 * @param coroutineScope 协程作用域
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    /** 当前选中的页面索引 */
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    /** 是否正在执行导航动画 */
    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    /** 当前显示的Screen */
    val currentScreen: Screen
        get() = rootTabAt(selectedPage)

    /**
     * 动画切换到指定Screen
     * 
     * @param screen 目标Screen
     */
    fun animateTo(screen: Screen) {
        animateToPage(screen.rootTabIndex())
    }

    /**
     * 动画切换到指定页面索引
     * 
     * @param targetIndex 目标页面索引
     */
    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        navJob =
            coroutineScope.launch {
                val myJob = coroutineContext.job
                try {
                    pagerState.scroll(MutatePriority.UserInput) {
                        // 计算滚动距离和动画时长
                        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(2)
                        val duration = 100 * distance + 100
                        val layoutInfo = pagerState.layoutInfo
                        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
                        val currentDistanceInPages =
                            targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
                        val scrollPixels = currentDistanceInPages * pageSize

                        // 执行平滑滚动动画
                        var previousValue = 0f
                        animate(
                            initialValue = 0f,
                            targetValue = scrollPixels,
                            animationSpec = tween(easing = EaseInOut, durationMillis = duration),
                        ) { currentValue, _ ->
                            previousValue += scrollBy(currentValue - previousValue)
                        }
                    }

                    // 确保到达目标页面
                    if (pagerState.currentPage != targetIndex) {
                        pagerState.scrollToPage(targetIndex)
                    }
                } finally {
                    if (navJob == myJob) {
                        isNavigating = false
                        if (pagerState.currentPage != targetIndex) {
                            selectedPage = pagerState.currentPage
                        }
                    }
                }
            }
    }

    /** 同步页面状态（处理用户手动滑动） */
    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

/**
 * 记住MainPagerState的Composable函数
 * 
 * @param pagerState PagerState实例
 * @param coroutineScope 协程作用域
 * @return MainPagerState实例
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MainPagerState = remember(pagerState, coroutineScope) { MainPagerState(pagerState, coroutineScope) }
