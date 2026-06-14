package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.entity.ExaminationEntity
import restarhalf.stellar.schedule.domain.model.Examination

fun ExaminationEntity.toDomain(): Examination {
    return Examination(
        courseNumber = courseNumber,
        courseName = courseName,
        time = time,
        examinationPlace = examinationPlace,
        zwh = zwh,
        ksbz = ksbz
    )
}

fun Examination.toEntity(semesterId: String): ExaminationEntity {
    return ExaminationEntity(
        courseNumber = courseNumber,
        courseName = courseName,
        time = time,
        examinationPlace = examinationPlace,
        zwh = zwh,
        ksbz = ksbz,
        semesterId = semesterId
    )
}
