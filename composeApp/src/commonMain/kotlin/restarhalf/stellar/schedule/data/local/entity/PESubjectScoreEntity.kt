package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity

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