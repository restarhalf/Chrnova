package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.entity.PEYearScoreEntity
import restarhalf.stellar.schedule.domain.model.PEYearScore

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