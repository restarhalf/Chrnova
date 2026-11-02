package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity

@Entity(tableName = "pe_detail_summary", primaryKeys = ["userNo", "schoolYear"])
data class PEDetailSummaryEntity(
    val userNo: String,
    val schoolYear: String,
    val totalScore: Double,
    val totalGrade: String
)
