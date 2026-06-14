package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.entity.PEStudentInfoEntity
import restarhalf.stellar.schedule.data.remote.PEStudentInfo

fun PEStudentInfo.toEntity() = PEStudentInfoEntity(
    id = "current",
    testCode = testCode,
    stuName = stuName,
    stdNumber = stdNumber
)

fun PEStudentInfoEntity.toDomain() = PEStudentInfo(
    testCode = testCode,
    stuName = stuName,
    stdNumber = stdNumber
)