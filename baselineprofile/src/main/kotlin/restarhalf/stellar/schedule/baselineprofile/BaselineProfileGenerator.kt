package restarhalf.stellar.schedule.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 在真机/模拟器上收集 Baseline Profile，覆盖冷启动、首次向导与底栏高频路径。
 *
 * 生成命令（需已连接设备）：
 * `./gradlew :androidApp:generateReleaseBaselineProfile`
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun collect() =
        baselineProfileRule.collect(
            packageName = PACKAGE_NAME,
            maxIterations = 5,
            stableIterations = 3,
        ) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 8_000)
            skipOnboardingIfNeeded()
            dismissBlockingDialogs()
            device.waitForIdle(1_000)

            // 首页：列表合成 + 公告入口
            visitTab(TAB_HOME) {
                scrollVertically()
                if (clickIfPresent(By.desc("公告"))) {
                    device.waitForIdle(600)
                    scrollVertically()
                    device.pressBackSafely()
                    device.waitForIdle(400)
                }
                scrollVertically()
            }

            // 课表：滚动 + 切周 + 周次选择器
            visitTab(TAB_SCHEDULE) {
                scrollVertically()
                swipeHorizontally()
                openScheduleWeekPicker()
                scrollVertically()
            }

            // 考务：考试/成绩双 Tab + 选修学分
            visitTab(TAB_EMS) {
                scrollVertically()
                clickIfPresent(By.text("考试"))
                device.waitForIdle(400)
                scrollVertically()
                clickIfPresent(By.text("成绩"))
                device.waitForIdle(500)
                scrollVertically()
                if (clickIfPresent(By.desc("选修学分"))) {
                    device.waitForIdle(600)
                    scrollVertically()
                    device.pressBackSafely()
                    device.waitForIdle(400)
                }
            }

            // 体测
            visitTab(TAB_PE) {
                scrollVertically()
                clickIfPresent(By.desc("二维码"))
                device.waitForIdle(400)
                device.pressBackSafely()
                scrollVertically()
            }

            // 设置：长列表滚动（账号/外观/关于等）
            visitTab(TAB_SETTINGS) {
                scrollVertically()
                scrollVertically()
                if (clickIfPresent(By.textContains("关于"))) {
                    device.waitForIdle(500)
                    scrollVertically()
                    device.pressBackSafely()
                }
            }

            // 再横滑一遍主 Pager，覆盖手势切 Tab 路径
            swipeThroughMainPager()
            clickIfPresent(By.text(TAB_HOME))
            device.waitForIdle(400)
            scrollVertically()
        }

    private fun MacrobenchmarkScope.skipOnboardingIfNeeded() {
        // 首次启动：欢迎 → 隐私 → 教务登录(跳过) → 体测登录(跳过) → Star → 进入软件
        repeat(10) {
            val progressed =
                clickIfPresent(By.desc("继续")) or
                    clickIfPresent(By.text("同意")) or
                    clickIfPresent(By.text("跳过")) or
                    clickIfPresent(By.text("下一步")) or
                    clickIfPresent(By.text("进入软件"))
            if (!progressed) return
            device.waitForIdle(700)
        }
    }

    private fun MacrobenchmarkScope.dismissBlockingDialogs() {
        // 更新弹窗 / 权限等阻塞层，有则点掉
        clickIfPresent(By.text("稍后"))
        clickIfPresent(By.text("关闭"))
        device.waitForIdle(300)
    }

    private fun MacrobenchmarkScope.visitTab(label: String, block: MacrobenchmarkScope.() -> Unit) {
        dismissBlockingDialogs()
        val tab =
            device.findObject(By.text(label))
                ?: device.findObject(By.textContains(label))
        if (tab != null) {
            runCatching {
                tab.click()
                device.wait(Until.hasObject(By.textContains(label)), 2_000)
            }
        }
        device.waitForIdle(700)
        this.block()
        device.waitForIdle(300)
    }

    private fun MacrobenchmarkScope.scrollVertically() {
        val target: UiObject2? =
            device.findObject(By.scrollable(true))
                ?: device.findObject(By.pkg(PACKAGE_NAME))
        if (target == null) return
        runCatching {
            target.setGestureMargin(device.displayWidth / 5)
            target.fling(Direction.DOWN)
            device.waitForIdle()
            target.fling(Direction.DOWN)
            device.waitForIdle()
            target.fling(Direction.UP)
            device.waitForIdle()
        }
    }

    private fun MacrobenchmarkScope.swipeHorizontally() {
        val w = device.displayWidth
        val h = device.displayHeight
        val y = h / 2
        runCatching {
            device.swipe(w * 3 / 4, y, w / 4, y, 25)
            device.waitForIdle(400)
            device.swipe(w / 4, y, w * 3 / 4, y, 25)
            device.waitForIdle(400)
        }
    }

    private fun MacrobenchmarkScope.openScheduleWeekPicker() {
        val title =
            device.findObject(By.textContains("第"))
                ?: device.findObject(By.textContains("假期中"))
        if (title != null) {
            runCatching {
                title.click()
                device.waitForIdle(600)
            }
        } else {
            // 顶栏标题区域打开周次选择
            runCatching {
                device.click(device.displayWidth / 2, (device.displayHeight * 0.08f).toInt())
                device.waitForIdle(600)
            }
        }
        // 点一个周次再关掉
        clickIfPresent(By.text("2"))
        clickIfPresent(By.text("1"))
        device.waitForIdle(400)
        device.pressBackSafely()
        device.waitForIdle(400)
    }

    private fun MacrobenchmarkScope.swipeThroughMainPager() {
        val w = device.displayWidth
        val h = device.displayHeight
        val y = h * 2 / 3
        repeat(4) {
            runCatching {
                device.swipe(w * 4 / 5, y, w / 5, y, 20)
                device.waitForIdle(350)
            }
            dismissBlockingDialogs()
            scrollVertically()
        }
        // 滑回首页方向
        repeat(4) {
            runCatching {
                device.swipe(w / 5, y, w * 4 / 5, y, 20)
                device.waitForIdle(300)
            }
        }
    }

    private fun UiDevice.findOrNull(selector: BySelector): UiObject2? =
        runCatching { findObject(selector) }.getOrNull()

    private fun MacrobenchmarkScope.clickIfPresent(selector: BySelector): Boolean {
        val obj = device.findOrNull(selector) ?: return false
        return runCatching {
            obj.click()
            device.waitForIdle(400)
            true
        }.getOrDefault(false)
    }

    private fun UiDevice.pressBackSafely() {
        runCatching { pressBack() }
        waitForIdle(300)
    }

    private companion object {
        const val PACKAGE_NAME = "restarhalf.stellar.schedule"
        const val TAB_HOME = "首页"
        const val TAB_SCHEDULE = "课程表"
        const val TAB_EMS = "考务"
        const val TAB_PE = "体测"
        const val TAB_SETTINGS = "设置"
    }
}
