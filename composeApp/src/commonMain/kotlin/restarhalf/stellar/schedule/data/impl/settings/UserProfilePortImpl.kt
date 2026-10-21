package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.UserProfilePort


class UserProfilePortImpl(
    private val settings: ObservableSettings,
) : UserProfilePort {


    override fun getUserAvatarUri(): String? {
        val uri = settings.getStringOrNull(SettingsKeys.USER_AVATAR_URI)
        return if (uri.isNullOrBlank()) null else uri
    }


    override fun setUserAvatarUri(uri: String?) {
        if (uri != null) {
            settings[SettingsKeys.USER_AVATAR_URI] = uri
        } else {
            settings[SettingsKeys.USER_AVATAR_URI] = ""
        }
    }


    override fun getUserNickname(): String? {
        val nickname = settings.getStringOrNull(SettingsKeys.USER_NICKNAME)
        return if (nickname.isNullOrBlank()) null else nickname
    }


    override fun setUserNickname(nickname: String?) {
        if (nickname != null) {
            settings[SettingsKeys.USER_NICKNAME] = nickname
        } else {
            settings[SettingsKeys.USER_NICKNAME] = ""
        }
    }
}
