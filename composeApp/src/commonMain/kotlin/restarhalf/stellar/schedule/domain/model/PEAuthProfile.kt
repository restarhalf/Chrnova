package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class PEAuthProfile(
    val stuName: String = "",
    val stdNumber: String = "",
    val testCode: String = "",
)
