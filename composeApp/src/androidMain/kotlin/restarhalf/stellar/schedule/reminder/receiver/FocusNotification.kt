package restarhalf.stellar.schedule.reminder.receiver

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import restarhalf.stellar.schedule.R

/**
 * 展开态超级岛里要显示的内容。
 *
 * 字段名对着 V3 模板的槽位命名，方便和下面 buildParam 里的 JSON 对照。
 */
internal data class FocusContent(
    /** baseInfo.title：主标题，课名 */
    val courseName: String,
    /** baseInfo.content：主标题下一行，起止时间 */
    val timeRange: String,
    /** hintInfo.content：左列标题，如 "即将上课" */
    val statusLabel: String,
    /** hintInfo.timerInfo.timerType：-1 倒计时 / 1 正计时 / 0 不带计时 */
    val timerType: Int,
    /** hintInfo.timerInfo.timerWhen：计时的基准时刻（epoch 毫秒，开课时间） */
    val timerWhenMs: Long,
    /** hintInfo.subContent：右列标题，如 "地点" */
    val placeLabel: String,
    /** hintInfo.subTitle：右列内容，如 "教科A-101" */
    val placeText: String,
    /** bigIslandArea.textInfo.title：deco 态只显示一行时的文本 */
    val islandTitle: String,
    /** ticker / aodTitle：状态栏与息屏上的那一行 */
    val ticker: String,
    /** shareData.shareContent：长按分享出去的整段文本 */
    val shareContent: String,
    /** 胶囊按钮；为空时不显示按钮 */
    val muteAction: Notification.Action?,
    /** 胶囊按钮文字 */
    val muteTitle: String,
)

/**
 * MIUI 焦点通知（超级岛 / Dynamic Island）。
 *
 * 入口是 miui.systemui.notification.focus.FocusNotifPreHandler：它读通知 extras 里的
 * miui.focus.param，先剥掉外层 param_v2 再交给 TemplateFactoryV3 渲染，
 * 所以参数必须写成 V3 的 baseInfo / hintInfo / timerInfo / actionInfo 这套，
 * 而不是早期 OS1 的 iconTextInfo + bigIslandArea.textInfo/imageTextInfo。
 *
 * 下面的字段顺序与取值直接照抄课程表超级岛的真实下发参数（从设备上 dump 出来的），
 * 包括 outer_glow 描边、business、shareData。这些都是 MIUI 的非公开协议。
 */
internal object FocusNotification {

    private const val KEY_PARAM = "miui.focus.param"
    private const val KEY_PICS = "miui.focus.pics"

    /** 顶层 type：系统按这个值认 V3 模板族 */
    private const val TYPE_V3 =
        "com.xzakota.hyper.notification.focus.FocusNotification.FocusTemplateFactory.V3"

    private const val KEY_ACTIONS = "miui.focus.actions"

    /**
     * 胶囊按钮走 Bundle 通道时用的键：ActionInfo.action 填这个字符串，
     * 系统 UI 就从这个 Bundle 里取出真的 Notification.Action（里面是真 PendingIntent）。
     */
    private const val ACTION_KEY = "miui.focus.action_mute"

    private const val PIC_APP_ICON = "miui.focus.pic_app_icon"
    private const val PIC_APP_ICON_SMALL = "miui.focus.pic_app_icon_small"
    private const val PIC_TICKER = "miui.focus.pic_ticker"
    private const val PIC_TICKER_DARK = "miui.focus.pic_ticker_dark"
    private const val PIC_EXPAND = "miui.focus.pic_expand"
    private const val PIC_EXPAND_DARK = "miui.focus.pic_expand_dark"
    private const val PIC_AOD = "miui.focus.pic_aod"
    private const val PIC_SHARE = "miui.focus.pic_share"

    /** picInfo / imageTextInfo 的 type：1 走默认图标位 */
    private const val PIC_TYPE_DEFAULT = 1

    /** baseInfo / hintInfo 的 type：2 是"标题 + 内容"的两行布局 */
    private const val INFO_TYPE_TWO_LINE = 2

    /** 描边特效，和课程表超级岛一致 */
    private const val OUT_EFFECT = "outer_glow"

    /** ActionInfo.type：2 = 右侧胶囊按钮（系统下发的默认动作模板用的也是 2） */
    private const val ACTION_TYPE_PILL = 2

    fun attach(
        context: Context,
        notification: Notification,
        content: FocusContent,
        nowMs: Long,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        notification.extras.putString(KEY_PARAM, buildParam(content, nowMs))
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

    private fun buildParam(content: FocusContent, nowMs: Long): String {
        val title = escape(content.courseName)
        val range = escape(content.timeRange)
        val status = escape(content.statusLabel)
        val placeLabel = escape(content.placeLabel)
        val place = escape(content.placeText)
        val islandTitle = escape(content.islandTitle)
        val ticker = escape(content.ticker)

        // hintInfo 里可选的两块：计时与按钮，都靠逗号结尾拼进对象
        val hintExtras = buildList {
            if (content.timerType != 0 && content.timerWhenMs > 0L) {
                add(
                    """"timerInfo": {"timerType": ${content.timerType}, """
                        + """"timerWhen": ${content.timerWhenMs}, "timerSystemCurrent": $nowMs}"""
                )
            }
            // 只给 action 键：系统 UI 会按它去 miui.focus.actions 里取真的 Notification.Action，
            // 点下去走的是我们自己签发的 PendingIntent，接收器不用对 systemui 开放。
            if (content.muteAction != null) {
                add(
                    """"actionInfo": {"action": "$ACTION_KEY", "type": $ACTION_TYPE_PILL, """
                        + """"actionTitle": "${escape(content.muteTitle)}"}"""
                )
            }
        }.joinToString(",\n                  ")

        // shareData 里的换行是 JSON 转义的 \n，不能被 escape() 压成空格
        return """
            {
              "type": "$TYPE_V3",
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
                "business": "course_schedule",
                "param_island": {
                  "islandProperty": 1,
                  "smallIslandArea": {},
                  "bigIslandArea": {
                    "textInfo": { "title": "$islandTitle", "showHighlightColor": true },
                    "imageTextInfoLeft": {
                      "type": $PIC_TYPE_DEFAULT,
                      "textInfo": { "title": "$place", "showHighlightColor": true },
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
                }
              }
            }
        """.trimIndent()
    }

    /** JSON 字符串里只有这几个字符需要转义；课程名一般是中文，转义以防引号/反斜杠。 */
    private fun escape(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
}
