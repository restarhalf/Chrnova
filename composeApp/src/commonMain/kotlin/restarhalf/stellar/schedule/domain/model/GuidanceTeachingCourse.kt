package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable


@Immutable
@Serializable
data class GuidanceTeachingCourse(
    
    val courseAttribute: String = "",
    
    val openSemester: String = "",
    
    val courseName: String = "",
    
    val totalHours: String = "",
    
    val courseCode: String = "",
    
    val kclbmc: String = "",
    
    val courseUnits: String = "",
    
    val whetherTest: String = "",
    
    val credit: String = "",
    
    val evaluationMode: String = "",
)
