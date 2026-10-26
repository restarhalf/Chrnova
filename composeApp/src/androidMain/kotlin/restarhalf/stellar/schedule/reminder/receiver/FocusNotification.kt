package restarhalf.stellar.schedule.reminder.receiver

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import restarhalf.stellar.schedule.R

internal data class FocusContent(
    val courseName: String,
    val timeRange: String,
    val statusLabel: String,
    val timerType: Int,
    val timerWhenMs: Long,
    val placeLabel: String,
    val placeText: String,
    val ticker: String,
    val shareContent: String,
    val muteAction: Notification.Action?,
    val muteTitle: String,
)

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

internal object FocusNotification {

    private const val KEY_PARAM = "miui.focus.param"
    private const val KEY_PICS = "miui.focus.pics"

    private const val TYPE_V3 =
        "com.xzakota.hyper.notification.focus.FocusNotification.FocusTemplateFactory.V3"

    private const val KEY_ACTIONS = "miui.focus.actions"

    private const val ACTION_KEY = "miui.focus.action_mute"

    private const val PIC_APP_ICON = "miui.focus.pic_app_icon"
    private const val PIC_APP_ICON_SMALL = "miui.focus.pic_app_icon_small"
    private const val PIC_TICKER = "miui.focus.pic_ticker"
    private const val PIC_TICKER_DARK = "miui.focus.pic_ticker_dark"
    private const val PIC_EXPAND = "miui.focus.pic_expand"
    private const val PIC_EXPAND_DARK = "miui.focus.pic_expand_dark"
    private const val PIC_AOD = "miui.focus.pic_aod"
    private const val PIC_SHARE = "miui.focus.pic_share"

    private const val PIC_TYPE_DEFAULT = 1

    private const val INFO_TYPE_TWO_LINE = 2

    private const val OUT_EFFECT = "outer_glow"

    private const val ACTION_TYPE_PILL = 2

    private const val PROTOCOL_V2 = 1

    fun attach(
        context: Context,
        notification: Notification,
        content: FocusContent,
        nowMs: Long,
    ) {
        val tier = FocusCapabilities.resolve(context)
        if (tier == FocusTier.NONE) return

        notification.extras.putString(KEY_PARAM, buildParam(content, nowMs, tier))
        notification.extras.putBundle(KEY_PICS, buildPics(context))
        notification.extras.putBundle(
            KEY_ACTIONS,
            Bundle().apply {
                content.muteAction?.let { putParcelable(ACTION_KEY, it) }
            },
        )
    }

    private fun buildPics(context: Context): Bundle {
        val icon = Icon.createWithResource(context, R.mipmap.ic_launcher)
        val small = Icon.createWithResource(context, R.mipmap.ic_launcher_round)
        return Bundle().apply {
            putParcelable(PIC_APP_ICON, icon)
            putParcelable(PIC_APP_ICON_SMALL, small)
            putParcelable(PIC_TICKER, icon)
            putParcelable(PIC_TICKER_DARK, icon)
            putParcelable(PIC_EXPAND, icon)
            putParcelable(PIC_EXPAND_DARK, icon)
            putParcelable(PIC_AOD, small)
            putParcelable(PIC_SHARE, icon)
        }
    }

    private fun buildParam(content: FocusContent, nowMs: Long, tier: FocusTier): String {
        val title = escape(content.courseName)
        val range = escape(content.timeRange)
        val status = escape(content.statusLabel)
        val placeLabel = escape(content.placeLabel)
        val place = escape(content.placeText)
        val ticker = escape(content.ticker)

        val hintExtras = buildList {
            if (content.timerType != 0 && content.timerWhenMs > 0L) {
                add(
                    """"timerInfo": {"timerType": ${content.timerType}, """
                        + """"timerWhen": ${content.timerWhenMs}, "timerSystemCurrent": $nowMs}"""
                )
            }
            if (content.muteAction != null) {
                add(
                    """"actionInfo": {"action": "$ACTION_KEY", "type": $ACTION_TYPE_PILL, """
                        + """"actionTitle": "${escape(content.muteTitle)}"}"""
                )
            }
        }.joinToString(",\n                  ")

        val islandBlock =
            if (tier == FocusTier.ISLAND) {
                """,
                "param_island": {
                  "islandProperty": 1,
                  "smallIslandArea": {},
                  "bigIslandArea": {
                    "textInfo": { "title": "$place", "showHighlightColor": true },
                    "imageTextInfoLeft": {
                      "type": $PIC_TYPE_DEFAULT,
                      "textInfo": { "title": "$title", "showHighlightColor": true },
                      "picInfo": { "type": $PIC_TYPE_DEFAULT }
                    }
                  },
                  "shareData": {
                    "title": "$title\n",
                    "content": "$place\n",
                    "pic": "$PIC_SHARE",
                    "shareContent": "${escape(content.shareContent)}"
                  },
                  "highlightColor": "#FFFFFF"
                }"""
            } else {
                ""
            }

        return """
            {
              "type": "$TYPE_V3",
              "protocol": $PROTOCOL_V2,
              "param_v2": {
                "ticker": "$ticker",
                "updatable": true,
                "enableFloat": false,
                "aodTitle": "$ticker",
                "baseInfo": {
                  "title": "$title",
                  "content": "$range",
                  "type": $INFO_TYPE_TWO_LINE
                },
                "hintInfo": {
                  "subTitle": "$place",
                  "content": "$status",
                  "subContent": "$placeLabel",
                  "type": $INFO_TYPE_TWO_LINE${if (hintExtras.isEmpty()) "" else ",\n                  $hintExtras"}
                },
                "picInfo": { "type": $PIC_TYPE_DEFAULT },
                "outEffectSrc": "$OUT_EFFECT",
                "business": "course_schedule"$islandBlock
              }
            }
        """.trimIndent()
    }

    private fun escape(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
}
