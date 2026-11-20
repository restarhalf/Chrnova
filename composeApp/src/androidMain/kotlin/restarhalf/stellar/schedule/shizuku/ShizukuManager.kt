package restarhalf.stellar.schedule.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import rikka.sui.Sui

/**
 * Shizuku 通道，照 Nexio 的 `ShizukuManager`：
 *
 * 超级岛通知发出前用特权把小米服务框架（XMSF）的网络临时掐掉，发完立刻恢复，
 * 避免焦点通知被服务端接管成普通推送而丢掉岛样式。
 *
 * Chrnova 只保留 binder 直连路径（[setPackageNetworkingEnabledViaBinder]）：
 * 效果与 Nexio 的用户服务一致，但不需要在本模块编 AIDL。
 */
object ShizukuManager {

    private const val TAG = "ShizukuManager"
    private const val XMSF_PACKAGE = "com.xiaomi.xmsf"

    /** ICONNECTIVITY_MANAGER 的 OEM 拦截链。 */
    private const val FIREWALL_CHAIN_OEM_DENY = 9

    @Volatile private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        runCatching {
            Sui.init(context.packageName)
            Shizuku.pingBinder()
        }.onFailure { Log.w(TAG, "Shizuku init failed", it) }
    }

    fun isShizukuRunning(): Boolean =
        runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun checkSelfPermission(): Boolean =
        runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun isAvailable(): Boolean = isShizukuRunning() && checkSelfPermission()

    fun requestPermission(callback: (Boolean) -> Unit) {
        if (!isShizukuRunning()) {
            callback(false)
            return
        }
        if (checkSelfPermission()) {
            callback(true)
            return
        }
        val listener =
            object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    callback(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        Shizuku.addRequestPermissionResultListener(listener)
        try {
            Shizuku.requestPermission(1001)
        } catch (e: Exception) {
            Shizuku.removeRequestPermissionResultListener(listener)
            callback(false)
        }
    }

    /** 开关小米服务框架的网络；Shizuku 不可用时返回 false，调用方直接照常发通知。 */
    fun setXmsfNetworkingEnabled(context: Context, enabled: Boolean): Boolean {
        if (!isAvailable()) return false
        return try {
            val xmsfUid = context.packageManager.getPackageUid(XMSF_PACKAGE, 0)
            setPackageNetworkingEnabledViaBinder(xmsfUid, enabled)
        } catch (e: Exception) {
            Log.w(TAG, "setXmsfNetworkingEnabled failed", e)
            false
        }
    }

    private fun setPackageNetworkingEnabledViaBinder(uid: Int, enabled: Boolean): Boolean =
        runCatching {
            val connectivityBinder = SystemServiceHelper.getSystemService("connectivity")
                ?: return@runCatching false
            val wrapped = ShizukuBinderWrapper(connectivityBinder)
            val manager =
                Class.forName("android.net.IConnectivityManager\$Stub")
                    .getMethod("asInterface", IBinder::class.java)
                    .invoke(null, wrapped)

            manager.javaClass
                .getMethod(
                    "setFirewallChainEnabled",
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
                .invoke(manager, FIREWALL_CHAIN_OEM_DENY, true)

            // 0 = ALLOW，2 = DENY
            val rule = if (enabled) 0 else 2
            manager.javaClass
                .getMethod(
                    "setUidFirewallRule",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                )
                .invoke(manager, FIREWALL_CHAIN_OEM_DENY, uid, rule)
            true
        }.getOrDefault(false)
}
