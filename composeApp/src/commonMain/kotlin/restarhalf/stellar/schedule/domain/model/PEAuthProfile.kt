package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class PEAuthProfile(
    @SerialName("stuName") val stuName: String = "",
    @SerialName("stdNumber") val stdNumber: String = "",
    @SerialName("testCode") val testCode: String = "",
)
