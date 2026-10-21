package restarhalf.stellar.schedule.domain.port


interface UserProfilePort {

    fun getUserAvatarUri(): String?

    fun setUserAvatarUri(uri: String?)


    fun getUserNickname(): String?

    fun setUserNickname(nickname: String?)
}
