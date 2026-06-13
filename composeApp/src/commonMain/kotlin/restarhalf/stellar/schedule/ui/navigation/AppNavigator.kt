package restarhalf.stellar.schedule.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class AppNavigator(
    val backStack: MutableList<NavKey>,
) {
    private val resultBus = mutableMapOf<String, MutableSharedFlow<Any>>()

    fun current(): NavKey? = backStack.lastOrNull()

    fun backStackSize(): Int = backStack.size

    fun push(key: NavKey) {
        backStack.add(key)
    }

    fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            cleanUpStaleChannels()
        }
    }

    fun replaceRoot(key: NavKey) {
        if (backStack.lastOrNull() == key && backStack.size == 1) return
        backStack.clear()
        backStack.add(key)
    }

    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun navigateForResult(key: NavKey, requestKey: String) {
        ensureChannel(requestKey)
        push(key)
    }

    fun <T : Any> setResult(requestKey: String, value: T) {
        ensureChannel(requestKey).tryEmit(value)
        pop()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> observeResult(requestKey: String): SharedFlow<T> =
        ensureChannel(requestKey) as SharedFlow<T>

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clearResult(requestKey: String) {
        ensureChannel(requestKey).resetReplayCache()
    }

    private fun ensureChannel(key: String): MutableSharedFlow<Any> =
        resultBus.getOrPut(key) { MutableSharedFlow(replay = 1, extraBufferCapacity = 0) }

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
