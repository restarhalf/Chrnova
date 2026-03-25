package restarhalf.stellar.schedule.ui.navigation

import androidx.navigation3.runtime.NavKey

class AppNavigator(
    val backStack: MutableList<NavKey>
) {

    fun current(): NavKey? = backStack.lastOrNull()

    fun backStackSize(): Int = backStack.size

    fun push(key: NavKey) {
        backStack.add(key)
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
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
}
