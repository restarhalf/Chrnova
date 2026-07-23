package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pe_detail_summary")
data class PEDetailSummaryEntity(
    @PrimaryKey val schoolYear: String,
    val totalScore: Double,
    val totalGrade: String
)
