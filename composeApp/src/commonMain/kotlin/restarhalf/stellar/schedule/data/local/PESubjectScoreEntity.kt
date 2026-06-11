package restarhalf.stellar.schedule.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pe_detail_scores", primaryKeys = ["schoolYear", "subjectId"])
data class PESubjectScoreEntity(
    val schoolYear: String,
    val subjectId: String,
    val subName: String,
    val result: String?,
    val score: Int?,
    val unit: String,
    val subRatio: String,
    val grade: String?,
    val isJoin: Int
)

@Entity(tableName = "pe_detail_summary")
data class PEDetailSummaryEntity(
    @PrimaryKey val schoolYear: String,
    val totalScore: Double,
    val totalGrade: String
)