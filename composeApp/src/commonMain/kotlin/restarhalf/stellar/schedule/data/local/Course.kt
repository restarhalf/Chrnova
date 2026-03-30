package restarhalf.stellar.schedule.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
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
    val targetWeek: Int = 0
)
