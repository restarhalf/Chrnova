package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "examinations",
    indices = [
        Index("semesterId"),
        Index("userNo"),
    ]
)
data class ExaminationEntity(
    
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    
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
