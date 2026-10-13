package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "courses",
    indices = [
        Index("semesterId"),
        Index("userNo"),
        Index("userNo", "semesterId"),
        Index("type"),
    ]
)
data class CourseEntity(
    
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    
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
)
