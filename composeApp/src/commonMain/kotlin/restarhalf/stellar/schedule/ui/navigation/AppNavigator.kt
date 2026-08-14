package restarhalf.stellar.schedule.ui.navigation

import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 应用导航器
 * 
 * 管理 miuix-nav 返回栈，提供 push、pop、popUntil 等导航操作。
 * 
 * @param backStack miuix-nav 返回栈（SnapshotStateList<NavKey>）
 */
class AppNavigator(
    val backStack: NavBackStack,
) {
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
     * 压入新页面，幂等：已在栈上的 key 直接跳过。
     * miuix-nav 拒绝重复的 contentKey，快速连点两次 push 同一路由会被运行时拒绝。
     * 需要多个实例的路由应携带唯一值。
     * 
     * @param key 要压入的页面NavKey
     */
    fun push(key: NavKey) {
        if (key !in backStack) {
            backStack.add(key)
        }
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
        }
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
}
