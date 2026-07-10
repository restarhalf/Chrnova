package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.entity.GradeEntity
import restarhalf.stellar.schedule.domain.model.GradeCourse

fun GradeEntity.toDomain(): GradeCourse {
    return GradeCourse(
        courseCode = courseCode,
        courseName = courseName,
        score = score,
        gradePoint = gradePoint,
        credit = credit,
        curriculumAttributes = curriculumAttributes,
        courseNature = courseNature,
        examName = examName,
        examinationNature = examinationNature,
        passStatus = passStatus,
        gradeLevel = gradeLevel,
        markFlag = markFlag,
        repeatSemester = repeatSemester,
        gradeId = gradeId,
        semester = semester,
        userNo = userNo
    )
}

fun GradeCourse.toEntity(userNo: String = this.userNo): GradeEntity {
    return GradeEntity(
        courseCode = courseCode,
        courseName = courseName,
        score = score,
        gradePoint = gradePoint,
        credit = credit,
        curriculumAttributes = curriculumAttributes,
        courseNature = courseNature,
        examName = examName,
        examinationNature = examinationNature,
        passStatus = passStatus,
        gradeLevel = gradeLevel,
        markFlag = markFlag,
        repeatSemester = repeatSemester,
        gradeId = gradeId,
        semester = semester,
        userNo = userNo
    )
}
