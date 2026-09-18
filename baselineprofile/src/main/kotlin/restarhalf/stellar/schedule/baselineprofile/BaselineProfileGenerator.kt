package restarhalf.stellar.schedule.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 在真机/模拟器上收集 Baseline Profile，覆盖冷启动与课表/设置高频路径。
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

            // 等主界面大致就绪（课表/首页内容出现）
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)

            // 主界面纵向滑动，覆盖课表/列表合成路径
            val root = device.findObject(By.pkg(PACKAGE_NAME))
            if (root != null) {
                runCatching {
                    root.setGestureMargin(device.displayWidth / 5)
                    root.fling(Direction.DOWN)
                    device.waitForIdle()
                    root.fling(Direction.UP)
                    device.waitForIdle()
                }
            }

            // 若存在底部导航，尝试进入设置页（有则点，无则忽略）
            val settingsTab =
                device.findObject(By.textContains("设置")) ?: device.findObject(By.textContains("Settings"))
            settingsTab?.click()
            device.waitForIdle(1_000)
        }

    private companion object {
        const val PACKAGE_NAME = "restarhalf.stellar.schedule"
    }
}
