/**
 * 小部件「点击 = 打开应用 + 刷新」的共用接线。
 *
 * 一个 PendingIntent 只能做一件事，所以「打开」和「刷新」不能各挂一个点击：
 * 这里改成：点击 → 带标记启动 Activity → Activity 检测标记后刷新全部小组件。
 */
package restarhalf.stellar.schedule.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object WidgetClickHelper {

    /** 标记「本次启动来自小部件点击」，由 MainActivity 消费后清掉 */
    const val EXTRA_FROM_WIDGET = "restarhalf.stellar.schedule.extra.FROM_WIDGET"

    /**
     * 打开应用并顺带刷新全部小组件的 PendingIntent。
     *
     * requestCode 固定为 0：[WidgetUpdateCache.updateInstalledWidgets] 刷的是所有实例，
     * 不需要按 widgetId 区分；同一份 Intent 复用反而能命中 PendingIntent 缓存，
     * 避免反复点击堆积。
     */
    fun launchAndRefreshPending(context: Context): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return refreshOnlyPending(context)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        launchIntent.putExtra(EXTRA_FROM_WIDGET, true)
        return PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 取不到启动 Intent（理论上不会）时的兜底：退化成只刷新 */
    private fun refreshOnlyPending(context: Context): PendingIntent {
        val refreshIntent = Intent(context, WidgetRefreshReceiver::class.java).apply {
            action = WidgetRefreshReceiver.ACTION_REFRESH_WIDGET
        }
        return PendingIntent.getBroadcast(
            context, 1, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 消费启动 Intent 上的标记：是小部件点来的就刷一遍，返回 true 表示已处理。
     *
     * 标记读掉即清，避免用户切走再回来时又白刷一次。
     */
    fun consumeLaunchFlag(intent: Intent?): Boolean {
        if (intent?.getBooleanExtra(EXTRA_FROM_WIDGET, false) != true) return false
        intent.removeExtra(EXTRA_FROM_WIDGET)
        return true
    }
}
