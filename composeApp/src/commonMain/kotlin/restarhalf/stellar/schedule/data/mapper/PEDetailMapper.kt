package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.entity.PEDetailSummaryEntity
import restarhalf.stellar.schedule.data.local.entity.PESubjectScoreEntity
import restarhalf.stellar.schedule.domain.model.PEDetailData
import restarhalf.stellar.schedule.domain.model.PESubjectScore

fun PEDetailData.toEntity(userNo: String, schoolYear: String) = listOf(
    PEDetailSummaryEntity(
        userNo = userNo,
        schoolYear = schoolYear,
        totalScore = totalScore,
        totalGrade = totalGrade
    )
) to dataArr.map { it.toEntity(userNo, schoolYear) }

fun PESubjectScore.toEntity(userNo: String, schoolYear: String) = PESubjectScoreEntity(
    userNo = userNo,
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
