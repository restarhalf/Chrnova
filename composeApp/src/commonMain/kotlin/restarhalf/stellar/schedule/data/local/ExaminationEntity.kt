package restarhalf.stellar.schedule.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "examinations")
data class ExaminationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseNumber: String = "",
    val courseName: String = "",
    val time: String = "",
    val examinationPlace: String = "",
    val zwh: String = "",
    val ksbz: String = "",
    val semesterId: String = ""
)
