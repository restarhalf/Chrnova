package restarhalf.stellar.schedule.reminder.receiver

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log

internal enum class FocusTier {
    NONE,
    FOCUS_ONLY,
    ISLAND,
}

internal object FocusCapabilities {

    private const val TAG = "FocusCapability"

    private const val PROP_ISLAND = "persist.sys.feature.island"

    private const val SETTING_PROTOCOL = "notification_focus_protocol"

    private const val AUTH_URI = "content://miui.statusbar.notification.public"

    private const val METHOD_CAN_SHOW_FOCUS = "canShowFocus"

    private const val EXTRA_PACKAGE = "package"

    fun resolve(context: Context): FocusTier {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return FocusTier.NONE

        val protocol = readProtocol(context)
        val island = hasIslandFeature()
        val canShow = canShowFocus(context)
        Log.d(TAG, "focus capability: protocol=$protocol island=$island canShowFocus=$canShow")
        if (protocol < 2 || !canShow) return FocusTier.NONE

        return if (protocol >= 3 && island) FocusTier.ISLAND else FocusTier.FOCUS_ONLY
    }

    private fun readProtocol(context: Context): Int =
        runCatching {
            Settings.System.getInt(context.contentResolver, SETTING_PROTOCOL, 0)
        }.getOrDefault(0)

    private fun hasIslandFeature(): Boolean =
        runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val getBoolean =
                clazz.getMethod("getBoolean", String::class.java, Boolean::class.javaPrimitiveType)
            getBoolean.invoke(null, PROP_ISLAND, false) as Boolean
        }.getOrDefault(false)

    private fun canShowFocus(context: Context): Boolean =
        try {
            val uri = Uri.parse(AUTH_URI)
            val extras = Bundle().apply { putString(EXTRA_PACKAGE, context.packageName) }
            val bundle = context.contentResolver.call(uri, METHOD_CAN_SHOW_FOCUS, null, extras)
            bundle?.getBoolean(METHOD_CAN_SHOW_FOCUS, false) ?: false
        } catch (t: Throwable) {
            Log.d(TAG, "canShowFocus threw: $t")
            false
        }
}