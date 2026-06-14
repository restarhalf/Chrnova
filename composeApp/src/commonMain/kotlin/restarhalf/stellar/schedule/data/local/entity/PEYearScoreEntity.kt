package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pe_scores")
data class PEYearScoreEntity(
    @PrimaryKey val schoolYear: String,
    val total: Double,
    val isFree: Int,
    val done: Int,
    val nums: Int
)