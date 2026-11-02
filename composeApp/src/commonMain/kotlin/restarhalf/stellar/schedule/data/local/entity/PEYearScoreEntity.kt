package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity

@Entity(tableName = "pe_scores", primaryKeys = ["userNo", "schoolYear"])
data class PEYearScoreEntity(
    val userNo: String,
    val schoolYear: String,
    val total: Double,
    val isFree: Int,
    val done: Int,
    val nums: Int
)
