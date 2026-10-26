package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getStringFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import restarhalf.stellar.schedule.data.remote.PEAuthStore
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort

@OptIn(ExperimentalSettingsApi::class)
class PEAuthPortImpl(
    private val authStore: PEAuthStore,
) : PEAuthPort {

    override fun observeToken(): Flow<String> {
        return authStore.getSettings().getStringFlow(PEAuthStore.KEY_TOKEN, "")
    }

    override fun observeProfile(): Flow<PEAuthProfile> {
        val settings = authStore.getSettings()
        return combine(
            settings.getStringFlow(PEAuthStore.KEY_STU_NAME, ""),
            settings.getStringFlow(PEAuthStore.KEY_STD_NUMBER, ""),
            settings.getStringFlow(PEAuthStore.KEY_TEST_CODE, "")
        ) { stuName, stdNumber, testCode ->
            PEAuthProfile(
                stuName = stuName, stdNumber = stdNumber, testCode = testCode
            )
        }
    }

    override fun setProfile(profile: PEAuthProfile) {
        authStore.setProfile(
            stuName = profile.stuName,
            stdNumber = profile.stdNumber,
            testCode = profile.testCode
        )
    }

    override fun setCredentials(username: String, password: String) {
        authStore.setCredentials(username, password)
    }

    override fun clear() {
        authStore.clear()
    }

    override fun getToken(): String? = authStore.getToken()

    override fun getUserId(): String? = authStore.getUserId()

    override fun getStdNumber(): String? = authStore.getStdNumber()
}
