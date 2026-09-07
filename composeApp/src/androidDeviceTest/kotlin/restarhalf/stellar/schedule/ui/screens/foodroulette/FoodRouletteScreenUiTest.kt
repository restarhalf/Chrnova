@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.foodroulette

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atiurin.ultron.core.compose.runUltronUiTest
import restarhalf.stellar.schedule.domain.model.Campus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 食堂轮盘 Compose UI 测试（Ultron KMP 入口）
 *
 * [FoodRouletteScreen] 无任何外部依赖（食物池为本地常量），直接渲染。
 * [FoodQRCodeScreen] 仅依赖 [FoodItem]，二维码用 qrose 真实生成。
 * 标准 ComposeUiTest API + waitUntil 异步等待（同公告测试模板）。
 * 注意：严禁嵌套 runUltronUiTest；SemanticsMatcher 扩展与 ComposeUiTest 环境互斥。
 */
class FoodRouletteScreenUiTest {

    private val poolNames = foodItemsFor(Campus.Jinshitan).map { it.name }.toSet()

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // region FoodRouletteScreen

    @Test
    fun 标题显示当前校区() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                FoodRouletteScreen(campus = Campus.Jinshitan, onBack = {}, onFoodSelected = {})
            }
        }
        waitText("今天吃什么 · 金石滩")
        onNodeWithText("今天吃什么 · 金石滩").assertIsDisplayed()
    }

    @Test
    fun 轮盘渲染食物池() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                FoodRouletteScreen(campus = Campus.Development, onBack = {}, onFoodSelected = {})
            }
        }
        waitText("火锅")
        onNodeWithText("火锅").assertIsDisplayed()
        onNodeWithText("今天吃什么 · 开发区").assertIsDisplayed()
    }

    @Test
    fun 点击就吃这个回调居中食物() = runUltronUiTest {
        val selected = mutableListOf<String>()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                FoodRouletteScreen(campus = Campus.Jinshitan, onBack = {}) { selected.add(it.name) }
            }
        }
        waitText("就吃这个！")
        onNodeWithText("就吃这个！").performClick()

        waitUntil(timeoutMillis = 2_000) { selected.isNotEmpty() }
        assertEquals(1, selected.size)
        assertTrue("回调食物 ${selected[0]} 不在食物池", selected[0] in poolNames)
    }

    @Test
    fun 随机按钮不直接触发选择回调() = runUltronUiTest {
        val selected = mutableListOf<String>()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                FoodRouletteScreen(campus = Campus.Jinshitan, onBack = {}) { selected.add(it.name) }
            }
        }
        waitText("随机")
        onNodeWithText("随机").performClick()

        // 随机按钮只滚动轮盘，不回调 onFoodSelected
        Thread.sleep(500)
        assertTrue("随机按钮不应触发 onFoodSelected，实际: $selected", selected.isEmpty())
    }

    @Test
    fun 点击单个食物项回调该食物() = runUltronUiTest {
        val selected = mutableListOf<String>()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                FoodRouletteScreen(campus = Campus.Jinshitan, onBack = {}) { selected.add(it.name) }
            }
        }
        waitText("火锅")
        onNodeWithText("火锅").performClick()

        assertEquals(listOf("火锅"), selected)
    }

    // endregion

    // region FoodQRCodeScreen

    @Test
    fun 二维码页无链接时显示今晚就吃它() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                FoodQRCodeScreen(food = FoodItem("火锅"), onBack = {})
            }
        }
        waitText("今晚就吃它！")
        onAllNodesWithText("火锅")[0].assertIsDisplayed()
    }

    @Test
    fun 二维码页有链接时显示扫码点餐() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                FoodQRCodeScreen(
                    food = FoodItem("奶茶", qrContent = "https://example.com/order"),
                    onBack = {},
                )
            }
        }
        waitText("扫码点餐")
        onAllNodesWithText("奶茶")[0].assertIsDisplayed()
        // 二维码 Image 以 contentDescription 暴露语义，hasText 匹配不到
        onNode(hasContentDescription("奶茶二维码")).assertIsDisplayed()
    }

    // endregion
}
