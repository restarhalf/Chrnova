package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.entity.ExaminationEntity
import restarhalf.stellar.schedule.domain.model.Examination

fun ExaminationEntity.toDomain(): Examination {
    return Examination(
        id = id,
        courseNumber = courseNumber,
        courseName = courseName,
        time = time,
        examinationPlace = examinationPlace,
        zwh = zwh,
        ksbz = ksbz,
        semesterId = semesterId,
        source = source,
        userNo = userNo,
    )
}

fun Examination.toEntity(semesterId: String = this.semesterId): ExaminationEntity {
    return ExaminationEntity(
        id = id,
        courseNumber = courseNumber,
        courseName = courseName,
        time = time,
        examinationPlace = examinationPlace,
        zwh = zwh,
        ksbz = ksbz,
        semesterId = semesterId,
        source = source,
        userNo = userNo,
    )
}
