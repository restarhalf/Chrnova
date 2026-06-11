package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.PEYearScoreEntity
import restarhalf.stellar.schedule.data.remote.PEYearScore

fun PEYearScore.toEntity() = PEYearScoreEntity(
    schoolYear = schoolYear,
    total = total,
    isFree = isFree,
    done = done,
    nums = nums
)

fun PEYearScoreEntity.toDomain() = PEYearScore(
    schoolYear = schoolYear,
    total = total,
    isFree = isFree,
    done = done,
    nums = nums
)