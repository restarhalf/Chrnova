package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.entity.PEDetailSummaryEntity
import restarhalf.stellar.schedule.data.local.entity.PESubjectScoreEntity
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PESubjectScore

fun PEDetailData.toEntity(schoolYear: String) = listOf(
    PEDetailSummaryEntity(
        schoolYear = schoolYear,
        totalScore = totalScore,
        totalGrade = totalGrade
    )
) to dataArr.map { it.toEntity(schoolYear) }

fun PESubjectScore.toEntity(schoolYear: String) = PESubjectScoreEntity(
    schoolYear = schoolYear,
    subjectId = subjectId,
    subName = subName,
    result = result,
    score = score,
    unit = unit,
    subRatio = subRatio,
    grade = grade,
    isJoin = isJoin
)

fun PEDetailSummaryEntity.toDomain(dataArr: List<PESubjectScore>) = PEDetailData(
    totalScore = totalScore,
    totalGrade = totalGrade,
    dataArr = dataArr
)

fun PESubjectScoreEntity.toDomain() = PESubjectScore(
    subjectId = subjectId,
    subName = subName,
    result = result,
    score = score,
    unit = unit,
    subRatio = subRatio,
    grade = grade,
    isJoin = isJoin
)