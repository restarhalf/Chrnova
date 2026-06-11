package restarhalf.stellar.schedule.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pe_student_info")
data class PEStudentInfoEntity(
    @PrimaryKey val id: String = "current",
    val testCode: String,
    val stuName: String,
    val stdNumber: String
)