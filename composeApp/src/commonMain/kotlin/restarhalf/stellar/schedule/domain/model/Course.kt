package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Course(
    
    val id: Long = 0,
    
    val name: String,
    
    val semesterId: String = "",
    
    val location: String,
    
    val teacher: String,
    
    val dayOfWeek: Int,
    
    val startSection: Int,
    
    val sectionCount: Int,
    
    val weeks: List<Int>,
    
    val color: String,
    
    val type: Int = 0,
    
    val remoteKey: String = "",
    
    val originRemoteKey: String? = null,
    
    val targetWeek: Int = 0,
    
    val userNo: String = "",
) {
    companion object {
        /** 同步课程未指定颜色时写入的占位色；渲染时应改用课程名调色板 */
        const val SYNC_PLACEHOLDER_COLOR = "#E6E6FA"
    }
}
