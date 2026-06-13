package restarhalf.stellar.schedule.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 应用导航器
 * 
 * 管理页面导航栈，提供push、pop、replace等导航操作。
 * 支持页面间结果传递。
 * 
 * @param backStack 导航栈，存储所有页面的NavKey
 */
class AppNavigator(
    val backStack: MutableList<NavKey>,
) {
    /** 结果传递通道映射 */
    private val resultBus = mutableMapOf<String, MutableSharedFlow<Any>>()

    /**
     * 获取当前页面
     * 
     * @return 当前页面的NavKey，栈为空时返回null
     */
    fun current(): NavKey? = backStack.lastOrNull()

    /**
     * 获取导航栈大小
     * 
     * @return 栈中页面数量
     */
    fun backStackSize(): Int = backStack.size

    /**
     * 压入新页面
     * 
     * @param key 要压入的页面NavKey
     */
    fun push(key: NavKey) {
        backStack.add(key)
    }

    /**
     * 替换当前页面
     * 
     * @param key 替换后的页面NavKey
     */
    fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    /** 弹出当前页面（返回上一页） */
    fun pop() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            cleanUpStaleChannels()
        }
    }

    /**
     * 替换根页面
     * 
     * @param key 新的根页面NavKey
     */
    fun replaceRoot(key: NavKey) {
        if (backStack.lastOrNull() == key && backStack.size == 1) return
        backStack.clear()
        backStack.add(key)
    }

    /**
     * 弹出页面直到满足条件
     * 
     * @param predicate 判断条件，返回true时停止弹出
     */
    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    /**
     * 带结果的页面导航
     * 
     * @param key 目标页面NavKey
     * @param requestKey 结果请求的唯一标识
     */
    fun navigateForResult(key: NavKey, requestKey: String) {
        ensureChannel(requestKey)
        push(key)
    }

    /**
     * 设置导航结果
     * 
     * @param requestKey 结果请求的唯一标识
     * @param value 结果值
     */
    fun <T : Any> setResult(requestKey: String, value: T) {
        ensureChannel(requestKey).tryEmit(value)
        pop()
    }

    /**
     * 观察导航结果
     * 
     * @param requestKey 结果请求的唯一标识
     * @return 结果SharedFlow
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> observeResult(requestKey: String): SharedFlow<T> =
        ensureChannel(requestKey) as SharedFlow<T>

    /**
     * 清除导航结果缓存
     * 
     * @param requestKey 结果请求的唯一标识
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clearResult(requestKey: String) {
        ensureChannel(requestKey).resetReplayCache()
    }

    /**
     * 获取或创建结果通道
     * 
     * @param key 通道标识
     * @return MutableSharedFlow实例
     */
    private fun ensureChannel(key: String): MutableSharedFlow<Any> =
        resultBus.getOrPut(key) { MutableSharedFlow(replay = 1, extraBufferCapacity = 0) }

    /** 清理无订阅者的通道 */
    private fun cleanUpStaleChannels() {
        val activeKeys = mutableSetOf<String>()
        for (key in resultBus.keys) {
            val flow = resultBus[key] ?: continue
            if (flow.subscriptionCount.value > 0) {
                activeKeys.add(key)
            }
        }
        resultBus.keys.retainAll(activeKeys)
    }
}
