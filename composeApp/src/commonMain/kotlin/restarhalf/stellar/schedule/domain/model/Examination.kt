package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Examination(
    
    val id: Long = 0,
    
    val courseNumber: String = "",
    
    val courseName: String = "",
    
    val time: String = "",
    
    val examinationPlace: String = "",
    
    val zwh: String = "",
    
    val ksbz: String = "",
    
    val semesterId: String = "",
    
    val source: String = "sync",
    
    val userNo: String = "",
)
