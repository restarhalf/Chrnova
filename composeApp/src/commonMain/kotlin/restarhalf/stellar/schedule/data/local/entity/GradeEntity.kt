package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "grades",
    indices = [
        Index("semester"),
        Index("userNo"),
        Index("userNo", "semester"),
    ]
)
data class GradeEntity(
    
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    
    val courseCode: String = "",
    
    val courseName: String = "",
    
    val score: String = "",
    
    val gradePoint: Double = 0.0,
    
    val credit: Double = 0.0,
    
    val curriculumAttributes: String = "",
    
    val courseNature: String = "",
    
    val examName: String = "",
    
    val examinationNature: String = "",
    
    val passStatus: String = "",
    
    val gradeLevel: String = "",
    
    val markFlag: String = "",
    
    val repeatSemester: String = "",
    
    val gradeId: String = "",
    
    val semester: String = "",
    
    val userNo: String = ""
)
