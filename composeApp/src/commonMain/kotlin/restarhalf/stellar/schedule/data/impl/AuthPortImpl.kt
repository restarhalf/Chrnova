package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getStringFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.domain.model.AuthProfile
import restarhalf.stellar.schedule.domain.port.AuthPort

@OptIn(ExperimentalSettingsApi::class)
class AuthPortImpl(
    private val authStore: JwxtAuthStore,
) : AuthPort {

    override fun observeToken(): Flow<String> {
        return authStore.getSettings().getStringFlow("token", "")
    }

    override fun observeProfile(): Flow<AuthProfile> {
        val settings = authStore.getSettings()
        return combine(
            settings.getStringFlow("name", ""),
            settings.getStringFlow("user_no", ""),
            settings.getStringFlow("cls_name", ""),
            settings.getStringFlow("academy_name", "")
        ) { name, userNo, clsName, academyName ->
            AuthProfile(
                name = name, userNo = userNo, clsName = clsName, academyName = academyName
            )
        }
    }

    override fun setCredentials(userNo: String, password: String) {
        authStore.setCredentials(userNo, password)
    }

    override fun clear() {
        authStore.clear()
    }
}
