package restarhalf.stellar.schedule.ui.navigation

import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey


class AppNavigator(
    val backStack: NavBackStack,
) {
    
    fun current(): NavKey? = backStack.lastOrNull()

    
    fun backStackSize(): Int = backStack.size

    
    fun push(key: NavKey) {
        if (key !in backStack) {
            backStack.add(key)
        }
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
        }
    }

    
    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}
