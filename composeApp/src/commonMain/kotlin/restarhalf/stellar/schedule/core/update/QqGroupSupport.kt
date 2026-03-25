package restarhalf.stellar.schedule.core.update

const val DEFAULT_QQ_GROUP_KEY = "wpb1TPVzJE1FGDTQTFvWu988ogFXOoqS"

private const val QQ_GROUP_UIN = "1084761691"
private const val QQ_GROUP_AUTH_KEY =
    "d4c1BU%2BS9ozb3l%2F4tWD%2FchM49M%2BiSZf6MTHbhGR0GLVBa4bN2tElX7HkBrZ6atq6"
private const val QQ_GROUP_AUTH_SIG =
    "EjF92D9ys0G2JGhWscqbnPll3Zs1W9%2BDYSyaST2hL6%2FRPSiTZKRaPMxQyKzt%2FWBu"

fun buildQqGroupWebUrl(key: String = DEFAULT_QQ_GROUP_KEY): String =
    if (key == DEFAULT_QQ_GROUP_KEY) {
        "https://qm.qq.com/cgi-bin/qm/qr?k=$DEFAULT_QQ_GROUP_KEY&jump_from=webapi&authKey=$QQ_GROUP_AUTH_KEY"
    } else {
        "https://qm.qq.com/cgi-bin/qm/qr?k=$key&jump_from=webapi"
    }

fun buildQqGroupIosUrl(): String =
    "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$QQ_GROUP_UIN&authSig=$QQ_GROUP_AUTH_SIG&card_type=group&source=external&jump_from=webapi"
